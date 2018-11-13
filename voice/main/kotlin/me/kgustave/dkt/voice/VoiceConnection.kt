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
@file:Suppress("PropertyName", "unused", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt.voice

import com.iwebpp.crypto.TweetNaclFast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import me.kgustave.dkt.core.entities.VoiceChannel
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.entities.VoiceChannelImpl
import me.kgustave.dkt.core.managers.VoiceManager
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.core.voice.PacketChannel
import me.kgustave.dkt.core.voice.VoiceMode
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.delegates.weak
import me.kgustave.dkt.voice.opus.AudioPacket
import me.kgustave.dkt.voice.opus.RTPHeaderByteLength
import me.kgustave.dkt.voice.opus.loadOpus
import me.kgustave.dkt.voice.websocket.Encryption
import me.kgustave.dkt.voice.websocket.VoiceInfo
import me.kgustave.dkt.voice.websocket.VoiceWebSocket
import opus.Opus
import opus.OpusEncoder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

@DktInternal
@ExperimentalVoiceAPI
@DktInternalExperiment
internal class VoiceConnection(
    private val manager: VoiceManagerImpl,
    endpoint: String,
    sessionId: String,
    token: String,
    channel: VoiceChannelImpl,
    bot: DiscordBotImpl
): CoroutineScope {
    override val coroutineContext = SupervisorJob()

    @Volatile private var _upd: DatagramSocket? = null
    private var _encoder: OpusEncoder? = null
    private var _channel by weak(channel)

    private var _senderJob: Job? = null

    @Volatile private var sender: VoiceManager.Sender? = null
    @Volatile private var couldReceive = false
    @Volatile private var speaking = false
    @Volatile private var speakingMode = VoiceMode.VOICE.raw
    @Volatile private var silenceCounter = 0
    private var sentSilenceOnConnect = false

    val bot by weak(bot)
    val encoder: OpusEncoder get() = _encoder ?: error("Encoder not available!")

//    val mainDispatcher      = newSingleThreadContext("VoiceWebSocket Main")
//    val heartbeatDispatcher = newSingleThreadContext("VoiceWebSocket Heartbeat")
//    val readerDispatcher    = newSingleThreadContext("VoiceWebSocket Reader")
//    val writerDispatcher    = newSingleThreadContext("VoiceWebSocket Writer")

    val websocket = VoiceWebSocket(VoiceInfo(this, channel.guild, endpoint, sessionId, token, true))

    val channel get() = _channel
    val udp: DatagramSocket get() = _upd ?: error("UDP is not connected!")
    val address: InetSocketAddress get() = websocket.address

    ////////////////////////
    // Internal Functions //
    ////////////////////////

    internal fun isUdpAvailable(): Boolean = _upd != null

    private fun attachSender(sender: VoiceManager.Sender?) {
        this.sender = sender
        if(websocket.isReady) {
            setupSender()
        }
    }

    private fun setSpeaking(raw: Int) {
        this.speaking = raw != 0

    }

    private fun encodeAudio(raw: ByteArray): ByteArray? {
        val result = runCatching { encoder.encode(raw, OpusFrameSize) }
        result.onFailure { t -> Log.error("Failed to encode audio", t) }
        return result.getOrNull()
    }

    ///////////////////
    // Private Setup //
    ///////////////////

    @Synchronized private fun setupSender() {
        val udp = this._upd
        val sender = this.sender
        val senderJob = this._senderJob
        if(udp != null && !udp.isClosed && sender != null && senderJob == null) {

        }
    }

    private inner class VoicePacketChannel: PacketChannel {
        override val address: InetSocketAddress get() = websocket.address
        override val channel: VoiceChannel get() = this@VoiceConnection.channel
        override val udp: DatagramSocket get() = this@VoiceConnection.udp

        private var sequence = 0.toShort()
        private var timestamp = 0
        private var nonce = 0L
        private var buffer = ByteBuffer.allocate(512)!!
        private var encryptionBuffer = ByteBuffer.allocate(512)!!
        private val nonceBuffer = ByteArray(TweetNaclFast.SecretBox.nonceLength)

        override suspend fun nextPacket(nowTalking: Boolean): DatagramPacket? {
            return nextPacketRaw(nowTalking)?.let { buff ->
                val data = buff.array()
                val offset = buff.arrayOffset()
                val position = buff.position()
                return@let DatagramPacket(data, offset, position - offset, address)
            }
        }

        override suspend fun nextPacketRaw(nowTalking: Boolean): ByteBuffer? {
            var next: ByteBuffer? = null
            runCatching {
                val sender = sender
                when {
                    sentSilenceOnConnect && sender != null && sender.canProvide() -> {
                        silenceCounter = -1
                        var raw = sender.provide20MsAudio()
                        if(raw == null || raw.isEmpty()) {
                            if(speaking && nowTalking) {
                                setSpeaking(0)
                            }
                        } else {
                            if(!sender.isOpus) {
                                raw = encodeAudio(raw)
                                if(raw == null) return@runCatching
                            }

                            next = getPacketData(raw)
                            if(!speaking)
                                setSpeaking(speakingMode)

                            incrementSeq()
                        }
                    }

                    silenceCounter > -1 -> {
                        next = getPacketData(Silence)
                        incrementSeq()
                        silenceCounter++

                        if(silenceCounter > 10) {
                            silenceCounter = -1
                            sentSilenceOnConnect = true
                        }
                    }

                    speaking && nowTalking -> setSpeaking(0)
                }
            }.onFailure { t ->
                Log.error("", t)
            }

            if(next != null)
                timestamp += OpusFrameSize

            return next
        }

        override fun onConnectionLost() {
            TODO("not implemented")
        }

        private fun getPacketData(data: ByteArray): ByteBuffer {
            ensureEncryptionData(data)
            val packet = AudioPacket(sequence, timestamp, websocket.ssrc, data, encryptionBuffer)

            val nlen = when(websocket.encryption) {
                Encryption.XSALSA20_POLY1305 -> 0
                Encryption.XSALSA20_POLY1305_LITE -> run {
                    if(nonce > UInt.MAX_VALUE.toLong()) {
                        nonce = 0
                        loadNextNonce(nonce)
                    } else {
                        nonce++
                        loadNextNonce(nonce)
                    }
                    return@run 4
                }
                Encryption.XSALSA20_POLY1305_SUFFIX -> {
                    ThreadLocalRandom.current().nextBytes(nonceBuffer)
                    TweetNaclFast.SecretBox.nonceLength
                }
            }

            buffer = packet.asEncryptedPacket(buffer, websocket.secretKey, nonceBuffer, nlen)
            return buffer
        }

        private fun encodeDataToOpus(raw: ByteArray): ByteArray? {
            if(_encoder == null) {
                if(!loadOpus()) return null
                val result = runCatching { OpusEncoder(OpusFrameSampleRate, OpusChannelCount, Opus.Constants.APPLICATION_AUDIO) }
                result.onFailure { Log.error("Failed to create encoder!", it) }
                _encoder = result.getOrNull() ?: return null
            }

            return encodeAudio(raw)
        }

        private fun ensureEncryptionData(data: ByteArray) {
            encryptionBuffer.clear()
            val currentCapacity = encryptionBuffer.remaining()
            val requiredCapacity = RTPHeaderByteLength + data.size
            if(currentCapacity < requiredCapacity)
                encryptionBuffer = ByteBuffer.allocate(requiredCapacity)
        }

        private fun loadNextNonce(nonce: Long) {
            nonceBuffer[0] = (nonce ushr 24 and 0xFF).toByte()
            nonceBuffer[1] = (nonce ushr 16 and 0xFF).toByte()
            nonceBuffer[2] = (nonce ushr 8 and 0xFF).toByte()
            nonceBuffer[3] = (nonce and 0xFF).toByte()
        }

        private fun incrementSeq() {
            if(sequence + 1 > Char.MAX_VALUE.toInt()) sequence = 0 else sequence++
        }
    }

    companion object {
        const val OpusFrameSampleRate = 48000 // Hz
        const val OpusFrameTime = 20 // ms
        const val OpusFrameSize = 960 // 48000 Hz for 20 ms of audio
        const val OpusChannelCount = 2

        private val Silence = byteArrayOf(0xF8.toByte(), 0xFF.toByte(), 0xFE.toByte())

        val Log = createLogger(VoiceConnection::class)
    }
}
