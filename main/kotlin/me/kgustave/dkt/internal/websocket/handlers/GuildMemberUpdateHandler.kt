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
import me.kgustave.dkt.internal.data.events.RawGuildMemberUpdateEvent
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.entities.MemberImpl
import me.kgustave.dkt.internal.entities.RoleImpl
import me.kgustave.dkt.internal.websocket.Payload
import java.util.*

internal class GuildMemberUpdateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val (guildId, roles, user, nick) = payload.d as RawGuildMemberUpdateEvent
        if(bot.guildSetupManager.isSettingUp(guildId)) {
            return bot.guildSetupManager.cache(guildId, payload)
        }

        val guild = bot.guildCache[guildId] ?: return logUnCachedEntity("GUILD_MEMBER_UPDATE", "guild", guildId)

        val member = guild.memberCache[user.id] as? MemberImpl

        if(member == null) {
            bot.eventCache.cache(EventCache.Type.MEMBER, guildId xor user.id, payload, this::handle)
            return logUnCachedEntity("GUILD_MEMBER_UPDATE", "member", user.id)
        }

        val currentRoles = member.roles
        val newRoles = roles.mapTo(LinkedList<RoleImpl>()) { roleId ->
            val role = guild.roleCache[roleId]
            if(role == null) {
                bot.eventCache.cache(EventCache.Type.ROLE, roleId, payload, this::handle)
                return logUnCachedEntity("GUILD_MEMBER_UPDATE", "role", roleId)
            }
            return@mapTo role
        }

        val removed = currentRoles.mapNotNull current@ { current ->
            val iterator = newRoles.iterator()
            for(new in iterator) {
                if(current == new) {
                    iterator.remove()
                    return@current null
                }
            }
            return@current current
        }

        if(removed.isNotEmpty()) {
            currentRoles.removeAll(removed)
            // TODO Event
        }

        if(newRoles.isNotEmpty()) {
            currentRoles.addAll(newRoles)
            // TODO Event
        }

        if(nick != null) {
            val oldNickname = member.nickname
            if(oldNickname != nick) {
                member.nickname = nick
                // TODO Event
            }
        }
    }
}
