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

/**
 * Represent's a typedef struct from the Opus library.
 *
 * Currently this consists of the following structs:
 *
 * - [OpusEncoder] an encoder that can process un-encoded audio data into opus-encoded audio data.
 * - [OpusDecoder] a decoder that can extract un-encoded audio data from opus-encoded audio data.
 *
 * All OpusStruct implementations should carry and wrap their [native pointer reference][PointerByReference]
 * as well as implement a [destruction handle][destroy] for freeing the native resources correctly as
 * described in the Opus reference API documentation.
 */
@ExperimentalOpus
interface OpusStruct {
    /**
     * The native pointer reference that corresponds to this struct.
     *
     * @throws IllegalStateException if this struct has been [destroyed][destroy].
     */
    val ptr: PointerByReference

    /**
     * Destroys this struct via it's [reference pointer][ptr].
     *
     * Note that any invocation of call to get [ptr] after this has been
     * called will result in an [IllegalStateException] being thrown!
     */
    fun destroy()
}
