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
package me.kgustave.dkt.entities

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer

enum class OnlineStatus {
    ONLINE,
    IDLE,
    DND,
    INVISIBLE,
    OFFLINE,
    UNKNOWN;

    @Serializer(forClass = OnlineStatus::class)
    companion object {
        @JvmStatic fun of(name: String) = values().firstOrNull { it.name == name.toUpperCase() } ?: UNKNOWN

        override fun deserialize(input: Decoder): OnlineStatus {
            val value = input.decodeString()

            return of(value)
        }

        override fun serialize(output: Encoder, obj: OnlineStatus) {
            require(obj != UNKNOWN) { "Cannot serialize OnlineStatus 'UNKNOWN'!" }

            output.encodeString(obj.name.toLowerCase())
        }
    }
}
