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
package opus.internal

import com.sun.jna.ptr.PointerByReference
import opus.ExperimentalOpus
import opus.Opus
import opus.OpusEncoder
import opus.exceptions.OpusEncoderException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

@UseExperimental(ExperimentalOpus::class)
internal class OpusEncoderImpl(sampleRate: Int, channels: Int, application: Int): OpusEncoder {
    private var _ptr: PointerByReference? = run {
        val error = IntBuffer.allocate(1)
        val ptr = Opus.lib.opus_encoder_create(sampleRate, channels, application, error)
        val e = error.get()
        if(e != Opus.Constants.OK && ptr == null) {
            throw OpusEncoderException("OpusEncoder could not be created", e)
        }
        return@run ptr
    }

    override val ptr: PointerByReference get() {
        return checkNotNull(_ptr) { "OpusEncoder pointer was nullified (this is likely due to a close)!" }
    }

    override fun encode(data: ByteArray, frameSize: Int): ByteArray {
        val pcm = ShortBuffer.allocate(data.size / 2) // for non-encoded data
        val encodedData = ByteBuffer.allocate(4096)
        for(i in 0 until data.size step 2) {
            val byte1 = 0x000000FF and data[i].toInt()
            val byte2 = 0x000000FF and data[i + 1].toInt()
            pcm.put((byte1 shl 8 or byte2).toShort())
        }

        pcm.flip()

        val result = Opus.lib.opus_encode(ptr, pcm, frameSize, encodedData, encodedData.capacity())

        // < 0 is an error
        if(result < 0) throw OpusEncoderException("OpusEncoder failed to encode audio", result)

        return ByteArray(result).also { encodedData.get(it) }
    }

    override fun destroy() {
        _ptr?.let {
            Opus.lib.opus_encoder_destroy(it)
            _ptr = null
        }
    }
}
