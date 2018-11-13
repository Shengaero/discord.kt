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
package me.kgustave.dkt.voice.websocket

import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI

@ExperimentalVoiceAPI
@DktInternalExperiment
internal enum class Encryption {

    XSALSA20_POLY1305_LITE,
    XSALSA20_POLY1305_SUFFIX,
    XSALSA20_POLY1305;

    val key get() = name.toLowerCase()

    companion object {
        @JvmStatic fun get(key: String) = Encryption.valueOf(key.toUpperCase())
    }
}
