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
package me.kgustave.dkt.core.internal.util

import kotlinx.io.IOException
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.writeFully
import kotlinx.io.streams.writePacket
import me.kgustave.dkt.core.internal.DktInternal
import java.io.ByteArrayOutputStream
import java.lang.ref.SoftReference
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

/**
 * A ZLib compressor/inflater used to cache binary frame content in
 * sequences and the inflate it into text at a later time after all
 * necessary frames have arrived.
 *
 * @author Kaidan Gustave
 */
@DktInternal
class ZLibCompressor {
    // Stored as a soft reference to allow for GC to clean up when appropriate.
    private var decompressBuffer: SoftReference<ByteArrayOutputStream>? = null

    private var readBuffer: BytePacketBuilder? = null

    private var inflater = Inflater()

    /**
     * Initializes the decompression buffer of this compressor ahead
     * of time as opposed to lazily. This is good if you are aware that
     * you'll be processing binary frame content eventually.
     */
    fun initDecompressBuffer() {
        decompressBuffer = SoftReference(compressionOutputStream())
    }

    /**
     * Adds the binary content of a frame to a buffer, possibly creating
     * one if necessary, and returns `true` if the binary content represents
     * the end of a fragmented payload. Returns `false` when the content
     * is too small or the suffix of the content isn't the proper ZLib suffix!
     *
     * Returns of this function that are `true` mean that [inflatePayload]
     * should be called with the same binary content provided as it's argument
     * as the one provided here as [content].
     *
     * @see ZLibSuffix
     * @see inflatePayload
     *
     * @return `true` if the binary content represents the end of the
     * current payload. `false` otherwise.
     */
    @Synchronized fun isContentCompletedBy(content: ByteArray): Boolean {
        // Get the packet builder or create a new one
        // This is null if the packet size is small enough
        //that we can read it in one run!
        val readBuffer = readBuffer ?: BytePacketBuilder().also { readBuffer = it }

        // Always try to write the content!
        // If this is null, that means our payload is
        //small enough to process without creating a buffer!
        readBuffer.writeFully(content)

        // The remaining content has not arrived yet!
        // We should return false so we can wait for
        //more binary frames to complete the packet!
        // If this returns true, the packet has been completed,
        //we can now read and return the text value of the packet
        //to be processed!
        return content.size >= 4 && suffixInt(content) == ZLibSuffix
    }

    @Deprecated("renamed to isContentCompletedBy", ReplaceWith("isContentCompletedBy(ba)"))
    @Synchronized fun isMessageCompletedByFrame(ba: ByteArray): Boolean = isContentCompletedBy(ba)

    /**
     * Resets the compressor.
     *
     * This should be done when the compressor is safely
     * inactive, and not when calls to [isContentCompletedBy]
     * are being made!
     */
    @Synchronized fun reset() {
        inflater = Inflater()
        decompressBuffer?.clear()
        readBuffer = null
    }

    /**
     * Inflates the currently stored binary content into text.
     *
     * This should be called after `true` is returned from [isContentCompletedBy]!
     *
     * @return The content of the compressor as a string.
     *
     * @throws java.io.IOException If an I/O error occurs.
     */
    @Throws(IOException::class) fun inflatePayload(content: ByteArray): String {
        val decompressBuffer = getDecompressBuffer()
        val decompressor = InflaterOutputStream(decompressBuffer, inflater)
        val readBuffer = this.readBuffer
        try {
            when(readBuffer) {
                // if this is null, we never created a readBuffer
                //because the first packet was the only packet for
                //this payload, so we just write to the decompressor.
                null -> decompressor.write(content)

                // if this is not null, we have read and cached
                //two or more packets into the readBuffer. Now
                //we are now writing the full content of the
                //readBuffer to the decompressor at once.
                else -> readBuffer.build().use(decompressor::writePacket)
            }
        } catch(e: IOException) {
            // make sure to clear the buffer first.
            decompressBuffer.reset()
            throw e
        } finally {
            readBuffer?.close()
            this.readBuffer = null
        }
        val text = decompressBuffer.toString("UTF-8")
        decompressBuffer.reset()
        return text
    }

    /////////////////////////////
    // Private implementations //
    /////////////////////////////

    private fun getDecompressBuffer(): ByteArrayOutputStream {
        // Attempt to the reference buffer.
        // If it was garbage collected, or if the
        //reference did not exist to begin with,
        //we should create, store and return a new one.
        return decompressBuffer?.get() ?: createAndStoreNewDecompressBuffer()
    }

    private fun createAndStoreNewDecompressBuffer(): ByteArrayOutputStream {
        val buffer = compressionOutputStream()
        decompressBuffer = SoftReference(buffer) // store the reference
        return buffer
    }

    private companion object {
        private const val ZLibSuffix = 0x0000FFFF

        /**
         * Creates a [ByteArrayOutputStream] with a size of `1024`.
         */
        @JvmStatic private fun compressionOutputStream() = ByteArrayOutputStream(1024)

        /**
         * Reads and calculates an int to compare with the [ZLibSuffix]
         * from the last 4 bytes of [ba].
         */
        @JvmStatic private fun suffixInt(ba: ByteArray): Int {
            val offset = ba.size - 4

            return (ba[offset + 3].toInt() and 0xFF) or
                (ba[offset + 2].toInt() and 0xFF shl 8) or
                (ba[offset + 1].toInt() and 0xFF shl 16) or
                (ba[offset    ].toInt() and 0xFF shl 24)
        }
    }
}
