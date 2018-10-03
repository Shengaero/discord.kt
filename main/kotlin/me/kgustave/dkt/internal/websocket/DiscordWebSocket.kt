/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package me.kgustave.dkt.internal.websocket

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketRawSession
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.URLProtocol
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readReason
import io.ktor.http.cio.websocket.readText
import io.ktor.util.moveToByteArray
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.whileSelect
import kotlinx.serialization.json.JSON
import me.kgustave.dkt.DiscordKt
import me.kgustave.dkt.internal.data.events.RawReadyEvent
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.internal.rest.restTask
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.currentTimeMs
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS

internal class DiscordWebSocket internal constructor(
    private val token: String,
    private val requester: Requester,
    private val compression: Boolean = false,
    private var shouldReconnect: Boolean = true
) {
    // When ktor officially supports WebSockets on OkHttp,
    //we should move to that instead of using CIO. It might
    //also be possible that we drop OkHttp entirely in favor
    //or CIO once it supports http 2.x (currently only 1.x
    //is supported.
    private val client = HttpClient(CIO) { install(WebSockets) }

    // rate-limiting
    @Volatile private var rateLimitPeriodReset = 0L
    @Volatile private var rateLimitPeriodWarningPrinted = false
    private val rateLimitPeriodUsage = atomic(0)

    // compression
    private val compressor = ZLibCompressor()

    // session management
    @Volatile private var connected = false
    @Volatile private var shutdown = false
    @Volatile private var identifyTime = 0L
    private lateinit var session: ClientWebSocketSession
    private var handleIdentifyRateLimit = false
    private var authenticated = false
    private var invalidation: Payload? = null
    private var sessionId: String? = null
    private var seq: Long? = null

    private val incoming get() = session.incoming
    private val outgoing get() = session.outgoing

    // heartbeat management
    private var lastHeartbeatTime = 0L
    private var lastAcknowledgeTime = 0L

    // misc
    val traces = hashSetOf<String>()
    val cloudflareRays = hashSetOf<String>()
    val ping get() = lastAcknowledgeTime - lastHeartbeatTime
    val sequence get() = seq ?: 0L
    val isConnected get() = connected

    // ## WebSocket Coroutine LifeCycle Graph ##
    //
    // [start] --> main (1)
    //               |
    //               + ----> +
    //               |       |
    //               |       + --> messager (2) <--- close -- +
    //               |       |                                |
    //               |       + --> heartbeat (3) <-- close -- +
    //               |       |                                |
    //               |       + --> reader (4) ------ +        |
    //               |       |                       |        |
    //               |       |                     close      |
    //               |       |                       |        |
    //               |       + ----> await close <-- +        |
    //               |                  |                     |
    //               |                  + ------------------- +
    //               |                  |
    //               + -- yes -- should reconnect? -- no --> [finish]
    //
    // 1) The main job, responsible for the sequential execution of
    // the websocket lifecycle. It is the parent to the messager,
    // heartbeat, and reader jobs, and thus cancelling it results
    // in the cancellation of those jobs.
    // 2) The messager channel, which acts as a queue for sending
    // messages to the websocket. This is kept on a separate
    // dispatcher from the main to properly send websocket messages
    // in the order they are queued.
    // 3) The heartbeat job, which sends heartbeats to the websocket
    // at the interval provided by HELLO payload.
    // 4) The reader job, which reads from the websocket during the
    // lifecycle until close is needed.

    private val mainDispatcher = newSingleThreadContext("DiscordWebSocket Main Thread")
    private val messagerDispatcher = newSingleThreadContext("DiscordWebSocket Messager Thread")
    private val heartbeatDispatcher = newSingleThreadContext("DiscordWebSocket Heartbeat Thread")
    private val readDispatcher = newSingleThreadContext("DiscordWebSocket Read Thread")

    private val mainScope = CoroutineScope(mainDispatcher)

    private lateinit var closeReason: CompletableDeferred<CloseOrder>
    private lateinit var main: Job
    private var messager: SendChannel<String>? = null
    private var heartbeat: Job? = null
    private var reader: Job? = null

    suspend fun connect() {
        val gateway = getGatewayBot()

        checkNotNull(gateway) { "Could not open a gateway connection because gateway info couldn't be retrieved!" }

        // TODO Maybe add some extra handling here?

        connect(gateway.url)
    }

    suspend fun connect(gatewayUrl: String) {
        // wait on session to start
        createNewSession(gatewayUrl)

        // we launch all child jobs under the same overarching
        //coroutine scope. This allows us to cancel all child jobs,
        //regardless of context, by simply cancelling the 'main'
        //parent job.
        main = mainScope.launch(mainDispatcher) { handleSession() }
    }

    suspend fun reconnect() {
        val gatewayInfo = getGatewayBot() ?: return
        createNewSession(gatewayInfo.url)

    }

    suspend fun shutdown() {
        shutdown = true
        shouldReconnect = false
        close(1000, "Shutting down")
        freeDispatchers()
    }

    //////////////////////
    // Session Handling //
    //////////////////////

    private suspend fun createNewSession(gatewayUrl: String) {
        // wait on session to start
        session = client.webSocketRawSession {
            method = Get
            headers[HttpHeaders.AcceptEncoding] = "gzip"
            url {
                protocol = URLProtocol.WSS
                host = gatewayUrl.removePrefix("wss://").removeSuffix("/")
                parameters["v"] = "${DiscordKt.GatewayVersion}"
                parameters["encoding"] = "json"
                if(compression) {
                    parameters["compress"] = "zlib-stream"
                    compressor.initDecompressBuffer()
                }
            }
        }

        Log.info("Connected to WebSocket!")

        val headers = session.call.response.headers
        val rays = headers.getAll("cf-ray") // rays3c
        if(rays != null && rays.isNotEmpty()) {
            val ray = rays[0]
            cloudflareRays += ray
            Log.trace("Received cf-ray: $ray")
        }

        session.masking = true
        closeReason = CompletableDeferred()
        connected = true
        rateLimitPeriodReset = currentTimeMs + 60000 // currentTimeMs + 60 seconds
    }

    private suspend fun handleSession() = currentScope {
        // now that we have connected, we should setup our messaging queue
        setupMessager()

        handleHello()

        // Now that we've started up the internal handlers for sending messages
        //to the websocket, we need to actually send the websocket info about
        //what account we are connecting as.
        // If we have a session ID stored, this
        //typically means that we are resuming.
        if(sessionId != null) {
            sendResume(sessionId!!)
        } else {
            sendIdentify()
            handleReady()
        }

        setupReader()

        handleDisconnect()
    }

    private suspend fun handleHello() {
        // Read the payload contents
        val payload = receiveNextPayload()

        // Make sure this is OP 10. This is effectively
        //making sure our cast will succeed.
        check(payload.op == OP.Hello) { "Expected OP ${OP.Hello} but received ${payload.op}!" }

        // return 'd' inner payload
        val hello = payload.d as Payload.Hello

        storeTraces(hello.trace, "HELLO", OP.Hello)
        setupHeartbeat(hello.heartbeatInterval)
    }

    private suspend fun handleReady() {
        val payload = receiveNextPayload()

        check(payload.op == OP.Event) { "Expected OP ${OP.Event} but received OP ${payload.op}!" }

        val ready = payload.d as RawReadyEvent

        sessionId = ready.sessionId
        storeTraces(ready.trace, "READY", OP.Event)

        Log.debug("Started with session ID: '$sessionId'")

        // TODO handle ready event further (store self-user, settings, etc)
    }

    private suspend fun handlePayload(payload: Payload) {
        when(payload.op) {
            OP.Event -> handleEvent(payload)
            OP.Heartbeat -> sendHeartbeat() // Received OP 1, heartbeat immediately
            OP.HeartbeatACK -> acknowledgeLastHeartbeat()
        }
    }

    private fun handleEvent(payload: Payload) {
        require(payload.op == OP.Event) { "Invalid payload OP: ${payload.op} (${OP.name(payload.op)}" }
        seq = payload.s
    }

    private suspend fun handleDisconnect() {
        val (closeReason, isClient) = closeReason.await()

        authenticated = true
        connected = false

        heartbeat?.let { heartbeat ->
            heartbeat.cancel()
            this.heartbeat = null
        }

        if(shutdown) {
            return Log.debug("Shutting down WebSocket...")
        }

        Log.debug("Encountered close reason: ${closeReason.code}")
        val closeCode = closeReason.code
        val closeStatus = CloseStatus.of(closeCode)

        val closeStatusMayReconnect = closeStatus?.mayReconnect ?: true
        val isInvalidate = invalidation?.d as? Boolean ?: false

        // invalidation's work is done
        invalidation = null

        if(!shouldReconnect || !closeStatusMayReconnect || (heartbeatDispatcher.executor as ExecutorService).isShutdown) {
            messager?.let { messager ->
                messager.close()
                this.messager = null
            }
            // TODO Handle Shutdown Further
        } else {
            compressor.reset()
            if(isInvalidate) invalidate()
            try {
                handleReconnect()
            } catch(t: Throwable) {

            }
        }
    }

    private suspend fun handleReconnect() {
        if(sessionId != null) {
            Log.warn("Disconnected from WebSocket! Attempting to resume session...")
            reconnect() // resume
        } else {
            if(handleIdentifyRateLimit) {
                val identifyRateLimit = currentTimeMs - (identifyTime + IdentifyDelay)
                if(identifyRateLimit > 0) {
                    Log.warn("RateLimit hit for OP ${OP.Identify}! Waiting ${identifyRateLimit}ms before reconnecting...")
                    delay(identifyRateLimit, MILLISECONDS)
                }

            }
        }
    }

    ///////////////
    // Job Setup //
    ///////////////

    private suspend fun setupReader() = currentScope {
        reader = launch(readDispatcher) {
            while(connected && !incoming.isClosedForReceive) {
                try {
                    val payload = receiveNextPayload()
                    handlePayload(payload)
                } catch(e: CancellationException) {
                    if(e is CloseCancellationException && !closeReason.isCompleted) {
                        val order = CloseOrder(e.reason, false) // only thrown when server sends close
                        closeReason.complete(order)
                    }
                    break
                } catch(t: Throwable) {
                    Log.error("Reader failed due to an unexpected exception!", t)
                }
            }
        }
    }

    private suspend fun setupMessager() = currentScope {
        messager = actor(messagerDispatcher) {
            while(connected && !isClosedForReceive) {
                try {
                    // We aren't authenticated yet, let's
                    //stick around and check back ever half
                    //a second until we are!
                    if(!authenticated) whileSelect {
                        onTimeout(500, MILLISECONDS) { !authenticated }
                    }

                    // Receiving null means that this channel is closed while we
                    //are awaiting the next request. If this happens, we should
                    //exit the loop and end this messager actor asap.
                    val text = receiveOrNull() ?: break

                    // send this message next
                    sendMessage(text, true)
                } catch(e: CancellationException) {
                    break
                } catch(t: Throwable) {
                    Log.error("Messager failed due to an unexpected exception!", t)
                }
            }
        }
    }

    private suspend fun setupHeartbeat(interval: Long): Unit = currentScope {
        // configure heartbeat job
        // this is a child of the current working scope
        //which should be `mainScope`.
        heartbeat = launch(heartbeatDispatcher) {
            while(connected && !outgoing.isClosedForSend) {
                try {
                    sendHeartbeat()
                    delay(interval, MILLISECONDS)
                } catch(e: CancellationException) {
                    // cancellation means the session has closed
                    break
                } catch(t: Throwable) {
                    Log.error("Heartbeat encountered an unexpected error: ", t)
                }
            }
        }
    }

    ////////////////////
    // Send Functions //
    ////////////////////

    private suspend fun sendHeartbeat() {
        val payload = Payload(op = OP.Heartbeat, d = seq)
        val text = JSON.stringify(payload)

        // heartbeats do not initially queue because they
        //are higher priority!
        if(!sendMessage(text, false))
            messager?.send(text)
        lastHeartbeatTime = currentTimeMs
    }

    private suspend fun sendIdentify() {
        val identify = Payload(
            op = OP.Identify,
            d = Payload.Identify(
                token = token,
                properties = Payload.Identify.Properties(
                    os = System.getProperty("os.name"),
                    browser = "Discord.kt",
                    device = "Discord.kt"
                ),
                presence = Payload.Identify.Presence(
                    status = "online",
                    afk = false,
                    game = Payload.Identify.Presence.Activity(
                        name = "with Discord.kt",
                        type = 0
                    )
                ),
                compress = compression,
                largeThreshold = 250
            )
        )

        Log.debug("Sending OP: ${OP.Identify} (Identify)...")
        sendPayload(identify, queue = false)
        identifyTime = currentTimeMs
    }

    private suspend fun sendResume(sessionId: String) {
        val resume = Payload(
            op = OP.Resume,
            d = Payload.Resume(
                token = token,
                sessionId = sessionId,
                seq = sequence
            )
        )

        Log.debug("Sending OP: ${OP.Resume} (Resume)...")
        sendPayload(resume, queue = false)
    }

    private suspend fun sendPayload(payload: Payload, queue: Boolean): Boolean {
        return sendMessage(JSON.stringify(payload), queue)
    }

    private suspend fun sendMessage(text: String, queue: Boolean): Boolean {
        if(!connected) return false

        Log.trace("-> $text")

        val now = currentTimeMs

        // time to reset
        if(rateLimitPeriodReset <= now) {
            rateLimitPeriodUsage.value = 0
            rateLimitPeriodReset = now + 60000 // now + 60 seconds
            rateLimitPeriodWarningPrinted = false
        }

        // safe to send
        // if we queue this request, we should delay until we're ready to send
        when {
            rateLimitPeriodUsage.value < MaxMessageSafeSendLimit ||
            (!queue && rateLimitPeriodUsage.value < MaxMessageSendLimit) -> {
                sendRateLimitedMessage(text)
                return true
            }

            queue -> {
                delay(rateLimitPeriodReset - currentTimeMs, MILLISECONDS)
                sendRateLimitedMessage(text)
                return true
            }

            !rateLimitPeriodWarningPrinted -> {
                // print warning once per rate limit period
                Log.warn("WebSocket rate limit was hit!")
                rateLimitPeriodWarningPrinted = true
            }
        }

        return false
    }

    private suspend fun sendRateLimitedMessage(text: String) {
        outgoing.send(Frame.Text(text))
        rateLimitPeriodUsage.incrementAndGet()
    }

    ///////////////////////
    // Receive Functions //
    ///////////////////////

    private fun acknowledgeLastHeartbeat() {
        lastAcknowledgeTime = currentTimeMs
        Log.debug("Heartbeat acknowledged! Gateway ping: $ping")
    }

    private fun invalidate() {
        sessionId = null
        authenticated = false
    }

    private tailrec suspend fun receiveNextPayload(): Payload {
        val frame = incoming.receive()
        val text = receivePayloadContent(frame)
        Log.trace("-> $text")
        val payload = JSON.nonstrict.parse<Payload>(text)

        // Discord sends us a couple of specific OP code types
        //at any given time, or in relation to stuff like the
        //heartbeat which is done in parallel to reading normally.
        // This means that in order to handle this while still
        //accurately predicting the expected returns of this
        //function in a sequential order as the lifecycle of
        //our connection implies, we need to handle these frames
        //and then recursively wait for another frame that should
        //what we want this function to return
        when(val op = payload.op) {
            // OP 1, Discord wants us to immediately send a heartbeat.
            OP.Heartbeat -> {
                Log.debug("Received OP $op, responding with heartbeat...")
                sendHeartbeat()
            }

            // OP 9, Discord has invalidated our session.
            OP.InvalidSession -> {
                invalidation = payload
                handleIdentifyRateLimit = handleIdentifyRateLimit && currentTimeMs - identifyTime < IdentifyDelay
                val canResume = payload.d as Boolean
                if(!canResume) invalidate() else {
                    // not authenticated anymore, even if we're resuming.
                    authenticated = false
                    Log.debug("Session invalidated! Attempting resume...")
                }

                // Note: This will result in a cancellation being thrown
                close(if(canResume) 4000 else 1000, "INVALIDATE_SESSION")
            }

            // OP 11, Discord acknowledged our last heartbeat.
            OP.HeartbeatACK -> acknowledgeLastHeartbeat()

            else -> return payload
        }

        return receiveNextPayload()
    }

    private tailrec suspend fun receivePayloadContent(frame: Frame, expectBinary: Boolean = false): String {
        when(frame) {
            is Frame.Text -> {
                // We have received a full text frame when we were expecting to
                //get the next fragment in a sequence via the recursive call!
                require(!expectBinary) {
                    "Frame provided was a text frame when a binary frame was expected! " +
                    "Please report this to the library maintainers!"
                }

                return frame.readText()
            }

            is Frame.Binary -> {
                val ba = frame.buffer.moveToByteArray()

                // The compressor has completed the
                //binary content with this frame!
                // We can now break out of the recursion
                //and return the joined content!
                if(compressor.isMessageCompletedByFrame(ba)) {
                    return compressor.inflatePayload(ba)
                }
            }

            is Frame.Ping -> { /* Do Nothing */ }
            is Frame.Pong -> { /* Do Nothing */ }
            is Frame.Close -> throw CloseCancellationException(frame.readReason() ?: CloseReason(1000, ""))
        }

        val nextFrame = incoming.receive()
        return receivePayloadContent(nextFrame)
    }

    //////////////////////
    // Helper Functions //
    //////////////////////

    private fun storeTraces(traces: List<String>, type: String, op: Int) {
        Log.debug("Storing _trace for OP $op ($type): $traces")
        this.traces.clear()
        this.traces += traces
    }

    private suspend fun getGatewayBot(): GatewayInfo? {
        val task = requester.restTask(Route.GetGatewayBot) { call ->
            call.response.receive<GatewayInfo>()
        }
        val info = runCatching { task.await() }
        return info.getOrNull()
    }

    /////////////////////
    // Close Functions //
    /////////////////////

    private suspend fun close(code: Short, message: String) {
        close(CloseReason(code, message))
    }

    private suspend fun close(reason: CloseReason) {
        if(::session.isInitialized) {
            // we are sending the close frame
            if(!outgoing.isClosedForSend) {
                outgoing.send(Frame.Close(reason))
            }

            if(!::main.isInitialized || !main.isActive) return

            // more efficient case is to kill the reader if it's still active,
            //and it will complete closeReason on it's own.
            val order = CloseOrder(reason, true)

            closeReason.complete(order)
            reader?.cancel()

            return main.join()

            //main.cancel()
        }
    }

    internal fun freeDispatchers() {
        mainDispatcher.close()
        messagerDispatcher.close()
        heartbeatDispatcher.close()
        readDispatcher.close()
    }

    internal suspend fun awaitTermination() {
        main.join()
    }

    ///////////////
    // Companion //
    ///////////////

    private companion object {
        private const val IdentifyDelay = 5 // seconds
        private const val MaxMessageSendLimit = 119
        private const val MaxMessageSafeSendLimit = MaxMessageSendLimit - 4

        private val Log = createLogger(DiscordWebSocket::class)
    }
}
