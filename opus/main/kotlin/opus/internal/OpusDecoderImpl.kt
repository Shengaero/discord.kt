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
@file:Suppress("DEPRECATION", "OverridingDeprecatedMember")
package opus.internal

import com.sun.jna.ptr.PointerByReference
import opus.ExperimentalOpus
import opus.Opus
import opus.OpusDecoder
import opus.exceptions.OpusDecoderException
import java.nio.IntBuffer
import java.nio.ShortBuffer

@UseExperimental(ExperimentalOpus::class)
internal class OpusDecoderImpl(sampleRate: Int, channels: Int): OpusDecoder {
    private var _ptr: PointerByReference? = run {
        val error = IntBuffer.allocate(1)
        val ptr = Opus.lib.opus_decoder_create(sampleRate, channels, error)
        val e = error.get()
        if(e != Opus.Constants.OK && ptr == null) {
            throw OpusDecoderException("OpusDecoder could not be created", e)
        }
        return@run ptr
    }

    override val ptr: PointerByReference get() {
        return checkNotNull(_ptr) { "OpusDecoder pointer was nullified (this is likely due to a close)!" }
    }

    override fun samplesIn(data: ByteArray, length: Int): Int {
        val result = Opus.lib.opus_decoder_get_nb_samples(ptr, data, length)

        when(result) {
            Opus.Constants.BAD_ARG ->
                throw IllegalArgumentException("Insufficient data was provided!")

            Opus.Constants.INVALID_PACKET ->
                throw IllegalArgumentException("Data provided is corrupted or of an unsupported type")

            else -> return result
        }
    }

    override fun decode(data: ByteArray?, frameSize: Int): ShortArray {
        val pcm = ShortBuffer.allocate(4096)
        val result = Opus.lib.opus_decode(ptr, data, data?.size ?: 0, pcm, frameSize, 0)

        // < 0 is an error
        if(result < 0) throw OpusDecoderException("OpusDecoder failed to decode audio", result)
        return ShortArray(result * 2).also { pcm.get(it) }
    }

    override fun destroy() {
        _ptr?.let {
            Opus.lib.opus_decoder_destroy(it)
            _ptr = null
        }
    }
}
