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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt.internal.websocket

/*
 * ## WebSocket Coroutine LifeCycle Graph ##
 *
 * [start] --> main (1)
 *               |
 *               |
 *               |
 *               + ----------> messager (2) <--- close ------ +
 *               |                                            |
 *               + ----- + --> heartbeat (3) <-- close -- +   |
 *               ^       |                                |   |
 *               |       + --> reader (4) ------ +        |   |
 *               |       |                       |        |   |
 *               |       |                     close      |   |
 *               |       |                       |        |   |
 *               |       + ----> await close <-- +        |   |
 *               |                  |                     |   |
 *               |                  + ------------------- +   |
 *               |                  |                         |
 *               + -- yes -- should reconnect? -- no -------- + --> [finish]
 *
 * 1) The main job, responsible for the sequential execution of
 *    the websocket lifecycle. It is the parent to the messager,
 *    heartbeat, and reader jobs, and thus cancelling it results
 *    in the cancellation of those jobs.
 * 2) The messager channel, which acts as a queue for sending
 *    messages to the websocket. This is kept on a separate
 *    dispatcher from the main to properly send websocket messages
 *    in the order they are queued.
 * 3) The heartbeat job, which sends heartbeats to the websocket
 *    at the interval provided by HELLO payload.
 * 4) The reader job, which reads from the websocket during the
 *    lifecycle until close is needed.
 */

import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readReason
import io.ktor.http.cio.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.DiscordKt
import me.kgustave.dkt.events.ReadyEvent
import me.kgustave.dkt.events.ReconnectEvent
import me.kgustave.dkt.events.ResumeEvent
import me.kgustave.dkt.events.ShutdownEvent
import me.kgustave.dkt.http.engine.websockets.ClientWebSocketSession
import me.kgustave.dkt.internal.data.events.RawReadyEvent
import me.kgustave.dkt.internal.data.events.RawResumeEvent
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.internal.impl.DiscordBotShardImpl
import me.kgustave.dkt.internal.websocket.handlers.WebSocketHandler
import me.kgustave.dkt.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

// FIXME This needs to be moved from 'internal.websocket' to just 'websocket'
class DiscordWebSocket internal constructor(
    internal val _bot: DiscordBotImpl,
    private val compression: Boolean = false,
    private var shouldReconnect: Boolean = true
) {
    private val client = _bot.httpClient

    //////////////////
    // RateLimiting //
    //////////////////

    @Volatile private var rateLimitPeriodWarningPrinted = false
    @Volatile private var rateLimitPeriodReset = 0L
    private val rateLimitPeriodUsage = atomic(0)

    ////////////////////////
    // Session Management //
    ////////////////////////

    @Volatile private var connection = WebSocketConnection(this, false)
    @Volatile private var connected = false
    @Volatile private var shutdown = false
    @Volatile private var identifyTime = 0L

    private lateinit var session: ClientWebSocketSession
    private var _trace = emptySet<String>()
    private var reconnectTimeoutDelay = 2 // In seconds
    private var sessionId: String? = null
    private var seq: Long? = null // stored as null because heartbeat ack

    private var authenticated = false
    private var initializing = true
    private var handleIdentifyRateLimit = false
    private var runningReadyOperations = false
    private var firstInitialization = true

    private val rays = hashSetOf<String>()
    private val compressor = ZLibCompressor()
    private val handlers = WebSocketHandler.newFullSet(_bot)

    private val incoming get() = session.incoming
    private val outgoing get() = session.outgoing

    internal val guildMembersChunkQueue = ConcurrentLinkedQueue<String>()
    internal val messageQueue = ConcurrentLinkedQueue<String>()
    internal val lock = ReentrantLock()

    ///////////////
    // Heartbeat //
    ///////////////

    private var lastHeartbeatTime = 0L
    private var lastAcknowledgeTime = 0L

    //////////////////////////
    // Dispatchers And Jobs //
    //////////////////////////

    private val closer = Channel<CloseOrder>()

    @Volatile private lateinit var _main: Job
    private var _messager: WebSocketMessager? = null
    @Volatile private var _heartbeat: Job? = null
    private lateinit var _reader: Job

    private val mainDispatcher      = newSingleThreadContext(threadName("Main"))
    private val messagerDispatcher  = newSingleThreadContext(threadName("Messager"))
    private val heartbeatDispatcher = newSingleThreadContext(threadName("Heartbeat"))
    private val readerDispatcher    = newSingleThreadContext(threadName("Reader"))

    private val mainScope = CoroutineScope(mainDispatcher)

    ///////////////////////
    // Public Properties //
    ///////////////////////

    /** The last `_trace` from any payload received containing one. */
    val trace get() = _trace

    /** A set of cloudflare rays received during the lifetime of this WebSocket. */
    val cloudflareRays: Set<String> get() = rays

    /** The [DiscordBot] that listens to this WebSocket. */
    val bot: DiscordBot get() = _bot

    /** The gateway response time for the last heartbeat. */
    val ping get() = lastAcknowledgeTime - lastHeartbeatTime

    /** The current event sequence number. */
    val sequence get() = seq ?: 0L

    /** Whether or not this WebSocket is connected. */
    val isConnected get() = connected

    val isReady get() = !initializing

    /** Whether or not this WebSocket is authenticated. */
    val isAuthenticated get() = authenticated

    /** Whether or not this WebSocket has shutdown. */
    val isShutdown get() = shutdown

    //////////////////////
    // Public Functions //
    //////////////////////

    suspend fun connect() {
        val gateway = getGatewayBot()

        // TODO Maybe add some extra handling here?

        makeConnection(gateway.url, isReconnect = false)
    }

    suspend fun reconnect() {
        if(isShutdown) {
            return dispatchShutdown(1000)
        }

        val gateway = getGatewayBot()

        var attempt = 0
        while(shouldReconnect) {
            attempt++
            _bot.status = DiscordBot.Status.WAITING_TO_RECONNECT
            delay(reconnectTimeoutDelay * 1000L) // seconds to milliseconds
            _bot.status = DiscordBot.Status.ATTEMPTING_TO_RECONNECT
            handleIdentifyRateLimit = false
            try {
                makeConnection(gateway.url, true)
                break
            } catch(t: RejectedExecutionException) {
                // RejectedExecutionException implies the bot has shutdown
                return dispatchShutdown(1000)
            } catch(e: RuntimeException) {
                reconnectTimeoutDelay = min(reconnectTimeoutDelay shl 1, _bot.maxReconnectDelay)
                Log.warn("Failed to reconnect (attempt $attempt)! Will retry in $reconnectTimeoutDelay seconds...")
            }
        }

        // This is where I want to take a second to write about just how we got here.
        // Keep in mind, the entire session is run on a loop/lifecycle, sequentially
        //executing and processing incoming frames in the order that the Discord API
        //documentation explains it should be handled.
        // This call is the recursion.
        handleSession(true)
    }

    suspend fun shutdown() {
        shutdown = true
        shouldReconnect = false
        bot.sessionHandler.dequeueConnection(connection)
        close(1000, "")
    }

    /////////////////////////////////
    // Internal Library Operations //
    /////////////////////////////////

    internal fun init() {
        try {
            bot.sessionHandler.queueConnection(connection)
        } catch(t: Throwable) {
            Log.error("Encountered an exception while initializing the WebSocket!")
            throw t
        }
    }

    internal fun finishReadyOperations() {
        if(initializing) {
            initializing = false
            runningReadyOperations = false

            if(firstInitialization) {
                firstInitialization = false

                Log.info("Finished Loading!")
                _bot.emit(ReadyEvent(bot))
            } else {
                Log.info("Finished (Re)Loading!")
                _bot.emit(ReconnectEvent(bot))
            }
        } else {
            Log.info("Finished Resuming Session!")
            _bot.emit(ResumeEvent(bot))
        }

        _bot.status = DiscordBot.Status.CONNECTED
    }

    internal fun updatePresence() {
        val payload = Payload(
            op = OP.StatusUpdate,
            d = bot.presence
        )

        queuePayload(payload)
    }

    internal fun sendGuildMemberRequest(request: Payload.GuildMemberRequest) {
        val payload = Payload(op = OP.RequestGuildMembers, d = request)
        val message = JsonParser.stringify(payload)
        doWhileLocked { guildMembersChunkQueue += message }
    }

    internal fun nullifyMessager() {
        // called after job is completed in messager
        _messager = null
    }

    internal fun freeDispatchers() {
        mainDispatcher.close()
    }

    internal fun handleEvent(payload: Payload) {
        expectOp(OP.Event, payload.op)
        seq = payload.s

        when(val d = payload.d) {
            is RawReadyEvent -> {
                checkEventType(EventType.READY, payload)

                _bot.status = DiscordBot.Status.LOADING_SUBSYSTEMS

                runningReadyOperations = true
                handleIdentifyRateLimit = false
                sessionId = d.sessionId
                d.trace?.let { storeTraces(it, "READY", OP.Event) }
            }

            is RawResumeEvent -> {
                checkEventType(EventType.RESUMED, payload)
                authenticated = true

                if(!runningReadyOperations) {
                    initializing = false
                    finishReadyOperations()
                } else {
                    Log.debug("Received resume while running ready operations!")
                    _bot.status = DiscordBot.Status.LOADING_SUBSYSTEMS
                }

                d.trace?.let { storeTraces(it, "READY", OP.Event) }
            }
        }

        val t = payload.t
        val handler = handlers[t]
        handler?.handle(payload)
    }

    //////////////////////
    // Session Handling //
    //////////////////////

    private suspend fun makeConnection(gatewayUrl: String, isReconnect: Boolean) {
        if(bot.status != DiscordBot.Status.ATTEMPTING_TO_RECONNECT) {
            _bot.status = DiscordBot.Status.CONNECTING_TO_WEBSOCKET
        }

        reject(isShutdown) { "WebSocket is shutdown!" }

        initializing = true

        // wait on session to start
        createNewSession(gatewayUrl)

        // If we are reconnecting, we don't need to go any
        //further because we are already running on the
        //main lifecycle scope in a loop.
        // This will be followed by handleSession being called.
        if(isReconnect) return

        // we launch all child jobs under the same overarching
        //coroutine scope. This allows us to cancel all child jobs,
        //regardless of context, by simply cancelling the 'main'
        //parent job.
        _main = mainScope.launch(mainDispatcher) { handleSession(false) }

        _main.invokeOnCompletion {
            if(it is CancellationException) {
                Log.debug("$mainDispatcher received a cancellation!")
            } else if(it !== null) {
                Log.error("$mainDispatcher encountered an unexpected exception!", it)
            }
        }
    }

    private suspend fun createNewSession(gatewayUrl: String) {
        // wait on session to start
        this.session = client.get(gatewayUrl) {
            headers[HttpHeaders.AcceptEncoding] = "gzip"
            url {
                parameters["v"] = "${DiscordKt.GatewayVersion}"
                parameters["encoding"] = "json"
                if(compression) {
                    parameters["compress"] = "zlib-stream"
                    compressor.initDecompressBuffer()
                }
            }
        }

        _bot.status = DiscordBot.Status.IDENTIFYING_SESSION

        when(sessionId) {
            null -> Log.info("Connected to WebSocket!")
            else -> Log.debug("Resumed connection to WebSocket!")
        }

        val headers = session.call.response.headers
        val rays = headers.getAll("cf-ray") // rays3c
        if(rays != null && rays.isNotEmpty()) {
            val ray = rays[0]
            this.rays += ray
            Log.trace("Received cf-ray: $ray")
        }

        session.masking = true
        connected = true

        rateLimitPeriodReset = currentTimeMs + 60000 // currentTimeMs + 60 seconds
    }

    private suspend fun handleSession(isReconnect: Boolean) {
        // now that we have connected, we should setup our messaging queue
        if(!isReconnect) setupMessager()

        handleHello()

        // Now that we've started up the internal handlers for sending messages
        //to the websocket, we need to actually send the websocket info about
        //what account we are connecting as.
        // If we have a session ID stored, this
        //typically means that we are resuming.
        val sessionId = sessionId
        if(sessionId != null && isReconnect) {
            sendResume(sessionId)
        } else {
            sendIdentify()
            handleReady()
        }

        setupReader()

        // Wait on close
        val (closeReason, isClient) = closer.receiveOrNull() ?: run {
            // our closer died? immediately terminate
            session.terminate()
            Log.error("Closer died?! Will shut down...")
            return@run CloseOrder(CloseReason(1000, ""), isClient = false)
        }

        Log.debug("CloseOrder fulfilled!")

        // Passed this point we should not in any way attempt to interact with the session,
        //as it is now completely terminated.
        _bot.status = DiscordBot.Status.DISCONNECTED

        authenticated = false
        connected = false

        _heartbeat?.cancel() // this will invoke the completion handler and set _heartbeat to null

        val closeCode = closeReason.code.toInt()
        val closeMessage = closeReason.message
        val closeStatus = CloseStatus.of(closeCode)

        Log.debug("Encountered close reason: ${closeReason.code}")

        val closeStatusMayReconnect = closeStatus?.mayReconnect ?: true
        val isInvalidate = isClient && closeCode == 1000 && closeMessage == "INVALIDATE_SESSION"
        val heartbeatDispatcherClosed = (heartbeatDispatcher.executor as ExecutorService).isShutdown

        if(!shouldReconnect || !closeStatusMayReconnect || heartbeatDispatcherClosed) {
            _messager?.close() // this will invoke the completion handler and set _messager to null
            closer.close()

            if(!closeStatusMayReconnect) {
                Log.error("WebSocket closed and cannot be recovered!")
            }

            if(shutdown) {
                messagerDispatcher.close()
                heartbeatDispatcher.close()
                readerDispatcher.close()
            }

            dispatchShutdown(closeCode)
            return Log.debug("WebSocket has shutdown completely!")
        }

        compressor.reset()
        Log.debug("Compressor reset...")

        if(isInvalidate) invalidate()

        try {
            handleReconnect()
        } catch(t: Throwable) {
            Log.error("Failed to resume session! Will invalidate and reconnect!")
            invalidate()
            queueReconnect()
        }
    }

    private suspend fun handleHello() {
        // Read the payload contents
        val payload = incoming.receiveNextPayload()

        // Make sure this is OP 10. This is effectively /making sure our cast will succeed.
        expectOp(OP.Hello, payload.op)

        // return 'd' inner payload
        val hello = payload.d as Payload.Hello

        storeTraces(hello.trace, "HELLO", OP.Hello)
        setupHeartbeat(hello.heartbeatInterval)
    }

    private suspend fun handleReady() {
        val payload = incoming.receiveNextPayload()
        handleEvent(payload)
    }

    private suspend fun handlePayload(payload: Payload) {
        when(payload.op) {
            OP.Event -> handleEvent(payload)
            OP.Heartbeat -> sendHeartbeat() // Received OP 1, heartbeat immediately
            OP.HeartbeatACK -> acknowledgeLastHeartbeat()
        }
    }

    private suspend fun handleReconnect() {
        if(sessionId != null) {
            Log.warn("Disconnected from WebSocket! Attempting to resume session...")
            reconnect() // resume
        } else {
            if(handleIdentifyRateLimit) {
                val identifyRateLimit = currentTimeMs - (identifyTime + (IdentifyDelay * 1000))
                if(identifyRateLimit > 0) {
                    Log.warn("RateLimit hit for OP ${OP.Identify}! Waiting ${identifyRateLimit}ms before reconnecting...")
                    delay(identifyRateLimit)
                }
            }
            queueReconnect()
        }
    }

    ///////////////
    // Job Setup //
    ///////////////

    private fun setupMessager() {
        if(_messager != null) return

        _messager = WebSocketMessager(mainScope, this, messagerDispatcher)
            .also(WebSocketMessager::start)
    }

    private fun setupReader() {
        _reader = mainScope.launch(readerDispatcher) {
            while(connected) {
                try {
                    val payload = incoming.receiveNextPayload()
                    handlePayload(payload)
                } catch(e: CancellationException) {
                    if(e is CloseCancellationException) { // only thrown when server sends close
                        return@launch closer.send(e.toCloseOrder())
                    }
                    break
                } catch(e: ClosedReceiveChannelException) {
                    break
                } catch(t: Throwable) {
                    Log.error("Reader encountered an unexpected exception!", t)
                }
            }
            return@launch closer.send(CloseOrder(CloseReason(1000, ""), isClient = true))
        }
    }

    private fun setupHeartbeat(interval: Long) {
        _heartbeat = mainScope.launch(heartbeatDispatcher) {
            while(connected) {
                try {
                    sendHeartbeat()
                    delay(interval)
                } catch(e: CancellationException) {
                    // cancellation -> session has closed
                    break
                } catch(e: ClosedSendChannelException) {
                    // closed send channel -> session has closed
                    break
                } catch(t: Throwable) {
                    Log.error("Heartbeat encountered an unexpected error: ", t)
                }
            }
        }

        _heartbeat?.invokeOnCompletion { _heartbeat = null }
    }

    ////////////////////
    // Send Functions //
    ////////////////////

    private suspend fun sendHeartbeat() {
        val payload = Payload(op = OP.Heartbeat, d = seq)

        // heartbeats do not initially queue because they
        //are higher priority!
        sendPayload(payload, queue = false)
        lastHeartbeatTime = currentTimeMs
    }

    private suspend fun sendIdentify() {
        val identify = Payload(
            op = OP.Identify,
            d = Payload.Identify(
                token = bot.token,
                properties = Payload.Identify.Properties(
                    os = System.getProperty("os.name"),
                    browser = "Discord.kt",
                    device = "Discord.kt"
                ),
                presence = _bot.presence,
                compress = compression,
                shard = (bot as? DiscordBotShardImpl)?.let { IntPair(it.shardId, it.shardTotal) },
                largeThreshold = 250
            )
        )

        Log.debug("Sending OP: ${OP.Identify} (Identify)...")
        sendPayload(identify, queue = false)
        handleIdentifyRateLimit = true
        identifyTime = currentTimeMs
        authenticated = true
        _bot.status = DiscordBot.Status.AWAITING_LOGIN_CONFIRMATION
    }

    private suspend fun sendResume(sessionId: String) {
        val resume = Payload(
            op = OP.Resume,
            d = Payload.Resume(
                token = bot.token,
                sessionId = sessionId,
                seq = sequence
            )
        )

        Log.debug("Sending OP: ${OP.Resume} (Resume)...")
        sendPayload(resume, queue = false)
        _bot.status = DiscordBot.Status.AWAITING_LOGIN_CONFIRMATION
    }

    private fun queuePayload(payload: Payload) {
        queueMessage(JsonParser.stringify(payload))
    }

    private fun queueMessage(text: String) {
        doWhileLocked { messageQueue.add(text) }
    }

    internal suspend fun sendPayload(payload: Payload, queue: Boolean): Boolean {
        return sendMessage(JsonParser.stringify(payload), queue)
    }

    internal suspend fun sendMessage(text: String, queue: Boolean): Boolean {
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
        val usage = rateLimitPeriodUsage.value
        when {
            usage < MaxMessageSafeSendLimit || (!queue && usage < MaxMessageSendLimit) -> {
                outgoing.send(Frame.Text(text))
                rateLimitPeriodUsage.incrementAndGet()
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

        doWhileLocked(guildMembersChunkQueue::clear)

        with(_bot) {
            textChannelCache.clear()
            voiceChannelCache.clear()
            categoryCache.clear()
            guildCache.clear()
            userCache.clear()
            privateChannelCache.clear()
            untrackedUsers.clear()
            untrackedPrivateChannels.clear()
            eventCache.clear()
            guildSetupManager.clear()
        }
    }

    private tailrec suspend fun ReceiveChannel<Frame>.receiveNextPayload(): Payload {
        // If this receives null we need to throw a normal cancellation,
        //as we do not know exactly why we are closing the receiving end
        val frame = receive()
        val text = receivePayloadContent(frame)
        Log.trace("-> $text")
        val payload = JsonParser.parse<Payload>(text)

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
                handleIdentifyRateLimit = handleIdentifyRateLimit && currentTimeMs - identifyTime < IdentifyDelay
                val canResume = payload.d as Boolean
                if(!canResume) invalidate() else {
                    // not authenticated anymore, even if we're resuming.
                    authenticated = false
                    Log.debug("Session invalidated! Attempting resume...")
                }

                close(if(canResume) 4000 else 1000, "INVALIDATE_SESSION")
            }

            // OP 11, Discord acknowledged our last heartbeat.
            OP.HeartbeatACK -> acknowledgeLastHeartbeat()

            else -> return payload
        }

        // This recursion will continue until a payload that does match
        //one of the above cases.
        return receiveNextPayload()
    }

    private tailrec suspend fun ReceiveChannel<Frame>.receivePayloadContent(frame: Frame, expectBinary: Boolean = false): String {
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
                val buffer = frame.buffer
                val ba = ByteArray(buffer.remaining()).also { buffer.get(it) }
                if(compressor.isMessageCompletedByFrame(ba)) {
                    // The compressor has completed the
                    //binary content with this frame!
                    // We can now break out of the recursion
                    //and return the joined content!
                    return compressor.inflatePayload(ba)
                }
            }

            // Frame.Ping and Frame.Pong are not handled, and (probably)
            //not even receivable from the WebSocket. We don't consider
            //these, but we still take the time to log if we get them.
            is Frame.Ping, is Frame.Pong -> Log.debug("Received a '${frame.frameType}' type frame!")
            is Frame.Close -> {
                val reason = frame.readReason() ?: CloseReason(1000, "")
                throw CloseCancellationException(reason, isClient = false)
            }
        }

        val next = receive()

        // Since text and close frames return immediately, and binary frames
        //return when completed, if we reach this point we should expect the
        //next frame to be a binary frame.
        return receivePayloadContent(next, expectBinary = true)
    }

    //////////////////////
    // Helper Functions //
    //////////////////////

    private fun storeTraces(trace: Set<String>, type: String, op: Int) {
        Log.debug("Storing _trace for OP $op ($type): $trace")
        _trace = trace
    }

    // TODO See if we can't move this to a better solution?
    private suspend fun getGatewayBot(): GatewayInfo {
        val info = runCatching { _bot.getGatewayInfo() }.getOrNull()

        reject(info == null) {
            "Could not open a gateway connection because " +
            "gateway info couldn't be retrieved!"
        }

        return info
    }

    private suspend fun queueReconnect() {
        try {
            _bot.status = DiscordBot.Status.RECONNECT_QUEUED
            connection = WebSocketConnection(this, true)
            bot.sessionHandler.queueConnection(connection)
        } catch(ex: IllegalStateException) {
            Log.error("Reconnection was rejected! Shutting down...")
            dispatchShutdown(1006)
            shutdown()
        }
    }

    private fun dispatchShutdown(withCode: Int = 1000) {
        if(bot.status != DiscordBot.Status.SHUTDOWN) {
            _bot.status = DiscordBot.Status.SHUTDOWN
        }

        _bot.emit(ShutdownEvent(bot, currentOffsetDateTime, withCode))
    }

    /////////////////////
    // Close Functions //
    /////////////////////

    internal suspend fun close(code: Short, message: String) = close(CloseReason(code, message))

    private suspend fun close(reason: CloseReason) {
        if(!::session.isInitialized) return

        // There is a chance that the main has not been initialized
        //yet, this would only occur if this is called while the
        //session is starting, but we check anyways just to be sure.
        // Additionally, we make sure the main is even active, so we
        //don't try to act upon a closed main somehow.
        if(!::_main.isInitialized || !_main.isActive) return

        outgoing.send(Frame.Close(reason))

        if(shutdown) {
            // we join so that the job fully completes before returning
            // this is because we want close to be completed before
            //any more action occurs.
            _main.join()
        }
    }

    ////////////////////
    // Lock Functions //
    ////////////////////

    internal fun <T> doWhileLocked(fn: () -> T) {
        try {
            lock.lockInterruptibly()
            fn()
        } catch(e: InterruptedException) {
            Log.error("Lock was interrupted!", e)
        } finally {
            unlockIfNeeded()
        }
    }

    internal fun unlockIfNeeded() {
        if(lock.isHeldByCurrentThread) lock.unlock()
    }

    ///////////////
    // Companion //
    ///////////////

    internal companion object {
        private const val IdentifyDelay = 5L // seconds
        private const val MaxMessageSendLimit = 119
        private const val MaxMessageSafeSendLimit = MaxMessageSendLimit - 4

        @JvmField internal val Log = createLogger(DiscordWebSocket::class)

        @JvmStatic private fun threadName(kind: String) = "DiscordWebSocket $kind Thread"

        @JvmStatic private fun checkEventType(expect: EventType, payload: Payload) {
            check(expect == payload.t) { "Failed event type check. Expected: '$expect', actual: '${payload.t}'" }
        }

        @JvmStatic private fun expectOp(expected: Int, actual: Int) {
            check(actual == expected) { "Expected OP $expected but received $actual!" }
        }
    }
}
