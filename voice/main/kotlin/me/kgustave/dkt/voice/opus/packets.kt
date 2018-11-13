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
@file:Suppress("FunctionName")
package me.kgustave.dkt.voice.opus

import com.iwebpp.crypto.TweetNaclFast
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import java.net.DatagramPacket
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.experimental.and

internal const val SequenceIndex = 2
internal const val TimestampIndex = 4
internal const val SSRCIndex = 8
internal const val RTPHeaderByteLength = 12

@ExperimentalVoiceAPI
@DktInternalExperiment
internal data class AudioPacket(
    val sequence: Short,
    val timestamp: Int,
    val ssrc: Int,
    val encodedAudio: ByteArray,
    val rawData: ByteArray
) {
    constructor(sequence: Short, timestamp: Int, ssrc: Int, encodedAudio: ByteArray, buffer: ByteBuffer):
        this(sequence, timestamp, ssrc, encodedAudio, buffer.loadBuffer(sequence, timestamp, ssrc, encodedAudio).array())

    private fun getNoncePadded(): ByteArray {
        val nonce = ByteArray(TweetNaclFast.SecretBox.nonceLength)
        System.arraycopy(rawData, 0, nonce, 0, RTPHeaderByteLength)
        return nonce
    }

    fun asEncryptedPacket(buffer: ByteBuffer, secretKey: ByteArray, nonce: ByteArray?, nlen: Int): ByteBuffer {
        val extendedNonce = nonce ?: getNoncePadded()

        val boxer = TweetNaclFast.SecretBox(secretKey)
        val encryptedAudio = boxer.box(encodedAudio, extendedNonce)

        buffer.clear()

        val capacity = rtpOffset(encryptedAudio.size + nlen)
        val targetBuffer = when {
            capacity > buffer.remaining() -> ByteBuffer.allocate(capacity)
            else -> buffer.loadBuffer(sequence, timestamp, ssrc, encryptedAudio)
        }

        if(nonce != null)
            targetBuffer.put(nonce, 0, nlen)

        return targetBuffer
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as AudioPacket

        if(sequence != other.sequence) return false
        if(timestamp != other.timestamp) return false
        if(ssrc != other.ssrc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequence.toInt()
        result = 31 * result + timestamp
        result = 31 * result + ssrc
        return result
    }
}

private fun ByteBuffer.loadBuffer(sequence: Short, timestamp: Int, ssrc: Int, encodedAudio: ByteArray): ByteBuffer = apply {
    put(0x80.toByte())
    put(0x78.toByte())
    putShort(sequence)
    putInt(timestamp)
    putInt(ssrc)
    put(encodedAudio)
}

private fun ByteArray.getShort(offset: Int): Short {
    return (this[offset].toInt() and 0xff shl 8 or (this[offset + 1].toInt() and 0xff)).toShort()
}

private fun rtpOffset(base: Int): Int = RTPHeaderByteLength + base

private fun payloadOffset(data: ByteArray, csrcLength: Int): Int {
    val headerLength = data.getShort(rtpOffset(2 + csrcLength))
    var i = RTPHeaderByteLength + 4 + csrcLength + headerLength * 4
    while(data[i].toInt() == 0)
        i++
    return i
}

@ExperimentalVoiceAPI
@DktInternalExperiment
internal val DatagramPacket.audioPacket get() = AudioPacket(data.copyOf())

@ExperimentalVoiceAPI
@DktInternalExperiment
internal fun AudioPacket(rawData: ByteArray): AudioPacket {
    val buffer = ByteBuffer.wrap(rawData)

    val sequence = buffer.getShort(SequenceIndex)
    val timestamp = buffer.getInt(TimestampIndex)
    val ssrc = buffer.getInt(SSRCIndex)

    val profile = buffer[0]
    val data = buffer.array()
    val csrcLength = (profile and 0x0f) * 10
    val hasExtension = (profile and 0x10) != 0.toByte()

    var offset = rtpOffset(csrcLength)
    val extension = if(hasExtension) data.getShort(offset) else 0
    if(hasExtension && extension == RTPHeaderByteLength.toShort()) {
        offset = payloadOffset(data, csrcLength)
    }

    val encodedSize = data.size - offset
    val encodedAudio = data.copyInto(ByteArray(encodedSize), startIndex = offset, endIndex = encodedSize)
    return AudioPacket(sequence, timestamp, ssrc, rawData, encodedAudio)
}
