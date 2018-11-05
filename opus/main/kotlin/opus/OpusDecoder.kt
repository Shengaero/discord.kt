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
package opus

import com.sun.jna.ptr.PointerByReference

@ExperimentalOpus
interface OpusDecoder: OpusStruct {
    /**
     * The native pointer reference that corresponds to this decoder.
     *
     * @throws IllegalStateException if this decoder has been [destroyed][destroy].
     */
    override val ptr: PointerByReference

    /**
     * Gets the number of samples in the provided Opus [data] with the specified [length].
     */
    fun samplesIn(data: ByteArray, length: Int = data.size): Int

    /**
     * Decodes the [data] provided with the given [frameSize] (default 20).
     *
     * @return The decoded data.
     */
    fun decode(data: ByteArray?, frameSize: Int = 20): ShortArray
}
