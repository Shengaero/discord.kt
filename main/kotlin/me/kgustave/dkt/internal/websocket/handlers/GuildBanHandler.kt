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

import me.kgustave.dkt.internal.cache.EventCache.Type.*
import me.kgustave.dkt.internal.data.events.RawGuildBanEvent
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.EventType.GUILD_BAN_ADD
import me.kgustave.dkt.internal.websocket.EventType.GUILD_BAN_REMOVE
import me.kgustave.dkt.internal.websocket.Payload

internal class GuildBanHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val type = payload.t
        require(type == GUILD_BAN_ADD || type == GUILD_BAN_REMOVE) { "Invalid payload.t provided!" }

        val (guildId, bannedUser) = payload.d as RawGuildBanEvent

        if(bot.guildSetupManager.isSettingUp(guildId)) return bot.guildSetupManager.cache(guildId, payload)
        val guild = bot.guildCache[guildId] ?: return bot.eventCache.cache(GUILD, guildId, payload, this::handle)
        val user = bot.entities.handleUntrackedUser(bannedUser, false)

        when(type) {
            GUILD_BAN_ADD -> { /* TODO Event */ }
            GUILD_BAN_REMOVE -> { /* TODO Event */ }

            else -> error("Invalid payload.t provided??????")
        }
    }
}
