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
@file:Suppress("unused")
package me.kgustave.dkt.entities

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer

enum class OnlineStatus {
    ONLINE,
    IDLE,
    DO_NOT_DISTURB("DND"),
    INVISIBLE,
    OFFLINE,
    UNKNOWN;

    val statusName: String

    // note: this is a compiler workaround for kotlinx.serialization
    constructor() { this.statusName = this.name }
    constructor(statusName: String) { this.statusName = statusName }

    @Serializer(OnlineStatus::class)
    companion object {
        @JvmStatic fun of(name: String): OnlineStatus {
            return values().firstOrNull { it.statusName == name.toUpperCase() } ?: UNKNOWN
        }

        override fun deserialize(input: Decoder): OnlineStatus = of(input.decodeString())
        override fun serialize(output: Encoder, obj: OnlineStatus) {
            output.encodeString(obj.statusName.toLowerCase())
        }
    }
}
