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
package me.kgustave.dkt.internal.websocket.handlers

import me.kgustave.dkt.internal.cache.EventCache
import me.kgustave.dkt.internal.data.RawMember
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.Payload

internal class GuildMemberAddHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val m = payload.d as RawMember
        requireNotNull(m.guildId) { "Guild ID not specified in GUILD_MEMBER_ADD payload!" }

        if(bot.guildSetupManager.addMember(m.guildId, m)) return

        val guild = bot.guildCache[m.guildId]

        if(guild == null) {
            bot.eventCache.cache(EventCache.Type.GUILD, m.guildId, payload, this::handle)
            return logUnCachedEntity("GUILD_MEMBER_ADD", "guild", m.guildId)
        }

        val member = bot.entities.handleMember(m, guild)

        // TODO Event
    }
}
