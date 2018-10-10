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

import me.kgustave.dkt.DiscordBot

interface Channel: Snowflake {
    val bot: DiscordBot
    val type: Channel.Type
    val guild: Guild?

    enum class Type(val type: Int, val isGuild: Boolean) {
        /** Type constant corresponding to a [TextChannel]. */
        TEXT(0, true),

        /** Type constant corresponding to a [PrivateChannel]. */
        PRIVATE(1, false),

        /** Type constant corresponding to a [VoiceChannel]. */
        VOICE(2, true),

        // Unsupported, bots cannot use groups.
        // If bots are allowed to use groups this will be implemented, otherwise
        // it will be left commented out.
        // GROUP(3, false)

        /** Type constant corresponding to a [Category]. */
        CATEGORY(4, true),

        UNKNOWN(-1, false);

        companion object {
            fun of(type: Int): Type {
                return values().firstOrNull { type == it.type } ?: UNKNOWN
            }
        }
    }
}
