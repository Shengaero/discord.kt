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

package me.kgustave.dkt.voice.websocket

import io.ktor.client.request.get
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readReason
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.internal.websocket.CloseCancellationException
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.http.engine.DiscordKtHttpEngineAPI
import me.kgustave.dkt.http.engine.websockets.DiscordWebSocketSession
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.currentTimeMs
import me.kgustave.dkt.voice.VoiceManagerImpl
import me.kgustave.dkt.voice.websocket.ConnectionPhase.*
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import kotlin.math.roundToLong

@ExperimentalVoiceAPI
@DktInternalExperiment
@UseExperimental(DiscordKtHttpEngineAPI::class)
internal class VoiceWebSocket(info: VoiceInfo): CoroutineScope by info.connection {
    @Volatile private lateinit var _encryption: Encryption
    private lateinit var _session: DiscordWebSocketSession
    private lateinit var _address: InetSocketAddress

    @Volatile private var phase = ConnectionPhase.NOT_CONNECTED
    @Volatile private var shutdown = false
    private var ready = false
    private var reconnecting = false
    private var _ssrc = 0
    private var _secretKey = ByteArray(SecretKeyLength)
    private var shouldReconnect = info.shouldReconnect

    private var heartbeat: Job? = null

    private val connection = info.connection
    private val guild = info.guild
    private val url = "wss://${info.endpoint}/?v=$VoiceGatewayVersion"
    private val sessionId = info.sessionId
    private val token = info.token
    private val client = bot.httpClient

    // FIXME
    // This is wayyyyy too expensive to spawn 4 threadpools per audio websocket!
    // We are going to need to centralize this eventually, probably on top of the
    //bot instance itself!
    private val mainDispatcher      = newSingleThreadContext("VoiceWebSocket Main")
    private val heartbeatDispatcher = newSingleThreadContext("VoiceWebSocket Heartbeat")
    private val readerDispatcher    = newSingleThreadContext("VoiceWebSocket Reader")
    private val writerDispatcher    = newSingleThreadContext("VoiceWebSocket Writer")

    private val bot get() = guild.bot
    private val session get() = _session
    private val outgoing get() = session.outgoing
    private val incoming get() = session.incoming

    val encryption get() = _encryption
    val secretKey get() = _secretKey
    val isReady get() = ready
    val ssrc get() = _ssrc
    val address get() = _address
    var ping = 0L

    suspend fun init() {
        attemptToConnect()

        launch(mainDispatcher) { handleSession() }
    }

    suspend fun shutdown(phase: ConnectionPhase) {
    }

    ////////////////////
    // Send Functions //
    ////////////////////

    suspend fun send(text: String) {
        outgoing.send(Frame.Text(text))
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
    suspend fun send(payload: VoicePayload) = send(JSON.stringify(payload))

    ///////////////
    // Lifecycle //
    ///////////////

    private suspend fun attemptToConnect() {
        check(!reconnecting && sessionIsInit()) {
            "An attempt to connect the VoiceWebSocket for Guild (ID: ${guild.id}) was made while reconnecting!"
        }

        phase = ConnectionPhase.CONNECTING_AWAITING_WEBSOCKET_CONNECT

        this._session = client.get(url)
        this.session.masking = true // enable masking
    }

    private suspend fun handleSession() = supervisorScope s@ {
        if(shutdown) {
            return@s outgoing.send(Frame.Close(DefaultClose))
        }

        var closeReason: CloseReason? = null
        while(!incoming.isClosedForReceive) {
            try {
                val payload = nextPayload()
                handlePayload(payload)
            } catch(t: CloseCancellationException) {
                closeReason = t.reason
                break
            } catch(t: CancellationException) {
                break
            } catch(t: ClosedReceiveChannelException) {
                break
            } catch(t: Throwable) {
                Log.error("Reader encountered an unexpected exception!", t)
            }
        }
    }

    private suspend fun CoroutineScope.handlePayload(payload: VoicePayload) {
        when(payload.op) {
            VOP.Hello -> {
                val d = payload.d as VoicePayload.Hello
                stopHeartbeat()
                startHeartbeat(adjustHeartbeatInterval(d.heartbeatInterval))
            }
            VOP.HeartbeatACK -> acknowledgeHeartbeat(payload.d as Long)
        }
    }

    private fun stopHeartbeat() {
        heartbeat?.cancel()
        heartbeat = null
    }

    private fun CoroutineScope.startHeartbeat(interval: Long) {
        if(heartbeat != null) {
            Log.warn("Attempting to set up VoiceWebSocket heartbeat while one is already present!")
        }

        launch(heartbeatDispatcher) { doHeartbeatLoop(interval) }
    }

    private tailrec suspend fun doHeartbeatLoop(interval: Long) {
        if(sessionIsInit() && session.isOpen) {
            send(VoicePayload(VOP.Heartbeat, currentTimeMs))
        }

        if(connection.isUdpAvailable() && !connection.udp.isClosed) {
            try {
                val packet = DatagramPacket(UDPHeartbeat, UDPHeartbeat.size, address)
                connection.udp.send(packet)
            } catch(t: NoRouteToHostException) {
                // Log
            }
        }

        delay(interval)
        doHeartbeatLoop(interval)
    }

    ///////////////////////
    // Receive Functions //
    ///////////////////////

    @UseExperimental(ImplicitReflectionSerializer::class)
    private suspend fun nextPayload(): VoicePayload {
        val text = nextPayloadContent()

        // log this to trace
        Log.trace("-> $text")

        return JSON.nonstrict.parse(text)
    }

    private tailrec suspend fun nextPayloadContent(): String {
        return when(val frame = incoming.receive()) {
            is Frame.Text -> frame.readText()

            is Frame.Close -> {
                val reason = frame.readReason() ?: DefaultClose
                throw CloseCancellationException(reason)
            }

            is Frame.Binary, is Frame.Ping, is Frame.Pong -> {
                Log.debug("Received a '${frame.frameType}' type frame from VoiceWebSocket!")
                nextPayloadContent()
            }
        }
    }

    /////////////////////
    // Close Functions //
    /////////////////////

    @UseExperimental(KtorExperimentalAPI::class)
    internal suspend fun close(phase: ConnectionPhase) {
        if(shutdown) return

        withLock { manager ->
            if(shutdown) return@withLock

            var truePhase = phase

            ready = false
            shutdown = true

            stopHeartbeat()

            if(connection.isUdpAvailable()) connection.udp.close()
            if(sessionIsInit()) session.close()

            connection//.shutdown() TODO

            val disconnectedChannel = manager.connectingChannel

            // nullify
            manager.connectingChannel = null

            val bot = bot

            if(truePhase == ERROR_LOST_CONNECTION) {
                val guild = bot.guildCache[guild.id]
                if(guild != null && guild.voiceChannelCache[connection.channel.id] != null) {
                    // This voice channel was deleted, so we change the true status
                    //to reflect this!
                    truePhase = DISCONNECTED_CHANNEL_DELETED
                }
            }

            this.phase = truePhase

            // Should we reconnect?
            if(shouldReconnect && truePhase.shouldReconnect && truePhase != AUDIO_REGION_CHANGE) {
                if(disconnectedChannel == null) {

                }
            }
        }
    }

    ///////////////
    // Utilities //
    ///////////////

    internal fun sessionIsInit() = ::_session.isInitialized

    internal inline fun withLock(block: (manager: VoiceManagerImpl) -> Unit) {
        val manager = this.guild.voiceManager as VoiceManagerImpl

        val result = runCatching {
            manager.connectionLock.lockInterruptibly()
            block(manager)
        }

        if(manager.connectionLock.isHeldByCurrentThread) {
            manager.connectionLock.unlock()
        }

        result.getOrThrow()
    }

    private fun acknowledgeHeartbeat(lastHeartbeatTime: Long) {
        ping = currentTimeMs - lastHeartbeatTime
        Log.debug("Heartbeat acknowledged! Voice Gateway (Guild ID: ${guild.id}) ping: $ping")
    }

    companion object {
        private const val VoiceGatewayVersion = 3
        private const val SecretKeyLength = 32

        private val UDPHeartbeat = byteArrayOf(0xC9.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
        private val DefaultClose = CloseReason(1000, "")

        private val Log = createLogger(VoiceWebSocket::class)

        @JvmStatic @Strictfp private fun adjustHeartbeatInterval(base: Long) = (base * 0.75).roundToLong()
    }
}
