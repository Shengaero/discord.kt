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
package me.kgustave.dkt.internal.websocket

import kotlinx.io.IOException
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.writeFully
import kotlinx.io.streams.writePacket
import java.io.ByteArrayOutputStream
import java.lang.ref.SoftReference
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

private const val ZLibSuffix = 0x0000FFFF
private const val COSSize = 1024

private fun compressionOutputStream() = ByteArrayOutputStream(COSSize)

private fun suffixInt(ba: ByteArray, offset: Int = ba.size - 4) = (
    (ba[offset + 3].toInt() and 0xFF) or
    (ba[offset + 2].toInt() and 0xFF shl 8) or
    (ba[offset + 1].toInt() and 0xFF shl 16) or
    (ba[offset].toInt() and 0xFF shl 24))

internal class ZLibCompressor {
    private val readLock = Any()
    private var context: Inflater? = Inflater()
    private var readBuffer: BytePacketBuilder? = null
    private lateinit var decompressBuffer: SoftReference<ByteArrayOutputStream>

    fun initDecompressBuffer() {
        decompressBuffer = SoftReference(compressionOutputStream())
    }

    fun isMessageCompletedByFrame(ba: ByteArray): Boolean {
        synchronized(readLock) {
            // Get the packet builder or create a new one
            // This is null if the packet size is small enough
            //that we can read it in one run!
            val readBuffer = readBuffer ?: when {
                ba.size < 4 -> null
                else -> BytePacketBuilder().also { readBuffer = it }
            }

            // always try to write the content!
            readBuffer?.writeFully(ba)

            // The remaining content has not arrived yet!
            // We should return false so we can wait for
            //more binary frames to complete the packet!
            if(ba.size < 4 || suffixInt(ba) != ZLibSuffix) return false

            // If we reach this point, the packet has been completed,
            //we can now read and return the text value of the packet
            //to be processed!
            return true
        }
    }

    fun inflatePayload(ba: ByteArray): String {
        val decompressBuffer = getDecompressBuffer()
        val decompressor = InflaterOutputStream(decompressBuffer, context)
        try {
            val readBuffer = readBuffer
            when(readBuffer) {
                null -> decompressor.write(ba)
                else -> readBuffer.build().use { decompressor.writePacket(it) }
            }
        } catch(e: IOException) {
            decompressBuffer.reset()
            throw e
        } finally {
            readBuffer?.close()
            readBuffer = null
        }
        val text = decompressBuffer.toString("UTF-8")
        decompressBuffer.reset()
        return text
    }

    fun reset() {
        synchronized(readLock) {
            context = Inflater()
            if(!::decompressBuffer.isInitialized) {
                decompressBuffer.clear()
            }
            readBuffer = null
        }
    }

    private fun getDecompressBuffer(): ByteArrayOutputStream {
        if(!::decompressBuffer.isInitialized) {
            val buffer = compressionOutputStream()
            decompressBuffer = SoftReference(buffer)
            return buffer
        }

        return decompressBuffer.get() ?: compressionOutputStream().also {
            decompressBuffer = SoftReference(it)
        }
    }
}
