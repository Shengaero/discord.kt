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
package opus.internal

import com.sun.jna.Library
import com.sun.jna.ptr.PointerByReference
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

// This is the internal interface we pass to JNA to construct bindings for.
// This should never be touched by code outside this library, as we try to
//convert and process the handling with a shallow layer of additional safety
//as well as translate the raw semantics of the opus library to kotlin ones.
internal interface LibOpus: Library {
    fun opus_encoder_get_size(channels: Int): Int

    fun opus_strerror(error: Int): String

    fun opus_get_version_string(): String

    fun opus_decoder_create(Fs: Int, channels: Int, error: IntBuffer): PointerByReference?

    fun opus_decode(
        st: PointerByReference,
        data: ByteArray?,
        len: Int,
        pcm: ShortBuffer,
        frame_size: Int,
        decode_fec: Int
    ): Int

    fun opus_decoder_get_nb_samples(dec: PointerByReference, packet: ByteArray, len: Int): Int

    fun opus_decoder_destroy(st: PointerByReference)

    fun opus_encoder_create(Fs: Int, channels: Int, application: Int, error: IntBuffer): PointerByReference?

    fun opus_encode(
        st: PointerByReference,
        pcm: ShortBuffer,
        frame_size: Int,
        data: ByteBuffer,
        max_data_bytes: Int
    ): Int

    fun opus_encoder_destroy(st: PointerByReference)
}
