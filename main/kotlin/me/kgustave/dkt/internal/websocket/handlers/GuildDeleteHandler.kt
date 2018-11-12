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
import me.kgustave.dkt.internal.data.RawUnavailableGuild
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.entities.GuildImpl
import me.kgustave.dkt.internal.websocket.Payload

internal class GuildDeleteHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val data = payload.d as RawUnavailableGuild

        if(bot.guildSetupManager.delete(data)) return

        val guild = bot.guildCache[data.id] ?: return logUnCachedEntity("GUILD_DELETE", "guild", data.id)

        // race condition?
        if(guild.unavailable && data.unavailable) return

        if(data.unavailable) {
            guild.unavailable = true
            // TODO Event
        }

        bot.guildCache -= guild.id
        guild.textChannelCache.values.forEach { bot.textChannelCache -= it.id }
        guild.voiceChannelCache.values.forEach { bot.voiceChannelCache -= it.id }
        guild.categoryCache.values.forEach { bot.categoryCache -= it.id }

        // copy to new set to avoid concurrent modification
        val memberIds = guild.memberCache.keys.toCollection(hashSetOf())
        bot.guildCache.forEach<GuildImpl> { g -> memberIds -= g.memberCache.keys }

        for(id in memberIds) {
            if(id == bot.self.id) continue
            val user = bot.userCache.remove(id) ?: continue

            user.privateChannel?.let { privateChannel ->
                user.untracked = true
                privateChannel.untracked = true
                bot.untrackedUsers[user.id] = user
                bot.untrackedPrivateChannels[privateChannel.id] = privateChannel
            }

            bot.eventCache.clear(EventCache.Type.USER, id)
        }

        // TODO Event
        bot.eventCache.clear(EventCache.Type.GUILD, guild.id)
    }
}
