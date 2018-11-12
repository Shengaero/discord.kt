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
package me.kgustave.dkt.core.internal.websocket.handlers

import me.kgustave.dkt.core.internal.data.events.RawGuildMembersChunkEvent
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.websocket.Payload

internal class GuildMembersChunkHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val d = payload.d
        require(d is RawGuildMembersChunkEvent) { "Inner payload data was not RawGuildMembersChunkEvent!" }

        val (guildId, members) = d

        bot.guildCache[guildId]?.let { guild ->
            val entities = bot.entities
            for(member in members) entities.handleMember(member, guild)
        }

        bot.guildSetupManager.chunk(guildId, members)
    }
}
