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
import me.kgustave.dkt.internal.data.events.RawGuildMemberRemoveEvent
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.entities.MemberImpl
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.internal.websocket.Payload

internal class GuildMemberRemoveHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val event = payload.d as RawGuildMemberRemoveEvent

        if(bot.guildSetupManager.removeMember(event.guildId, event.user)) return

        val guild = bot.guildCache[event.guildId] ?: return DiscordWebSocket.Log.debug(
            "Received GUILD_MEMBER_REMOVE event for guild that was not cached! " +
            "This is likely a guild that was left!"
        )

        val member = guild.memberCache.remove(event.user.id) as? MemberImpl ?:
                     return logUnCachedEntity("GUILD_MEMBER_REMOVE", "member", event.user.id)

        val voiceState = member.voiceState
        val connectedVoiceChannel = voiceState.channel

        if(connectedVoiceChannel != null) {
            voiceState.channel = null
            connectedVoiceChannel.connectedMembers.remove(member.user.id)
            // TODO Event
        }

        // the member is not us and it's not in any other guilds, so we need to remove it
        if(member.user.id != bot.self.id && bot.guildCache.values.none { member.user.id in it.memberCache }) {
            val user = bot.userCache.remove(member.user.id)!!
            user.privateChannel?.let { privateChannel ->
                user.untracked = true
                privateChannel.untracked = true
                bot.untrackedUsers[user.id] = user
                bot.untrackedPrivateChannels[privateChannel.id] = privateChannel
            }
            bot.eventCache.clear(EventCache.Type.USER, user.id)
        }

        // TODO Event
    }
}
