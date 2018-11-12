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

import me.kgustave.dkt.entities.Channel
import me.kgustave.dkt.internal.data.RawChannel
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.internal.websocket.Payload

internal class ChannelDeleteHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val channel = payload.d as RawChannel
        val type = Channel.Type.of(channel.type)
        val guildId = channel.guildId
        if(type.isGuild) {
            requireNotNull(guildId)
            if(bot.guildSetupManager.isSettingUp(guildId)) {
                return bot.guildSetupManager.cache(guildId, payload)
            }
        }

        val channelId = channel.id

        when(type) {
            Channel.Type.TEXT -> {
                val guild = bot.guildCache[guildId!!]
                checkNotNull(guild) { "Guild was not cached (ID: $guildId)" }

                val text = guild.textChannelCache.remove(channelId) ?:
                           return logUnCachedEntity("CHANNEL_DELETE", "text channel", channelId)

                bot.textChannelCache.remove(channelId)

                // TODO Event
            }

            Channel.Type.VOICE -> {
                val guild = bot.guildCache[guildId!!]
                checkNotNull(guild) { "Guild was not cached (ID: $guildId)" }

                val voice = guild.voiceChannelCache.remove(channelId) ?:
                            return logUnCachedEntity("CHANNEL_DELETE", "voice channel", channelId)

                bot.voiceChannelCache.remove(channelId)

                // TODO Event
            }

            Channel.Type.CATEGORY -> {
                val guild = bot.guildCache[guildId!!]
                checkNotNull(guild) { "Guild was not cached (ID: $guildId)" }

                val category = guild.categoryCache.remove(channelId) ?:
                               return logUnCachedEntity("CHANNEL_DELETE", "category", channelId)

                bot.categoryCache.remove(channelId)

                // TODO Event
            }

            Channel.Type.PRIVATE -> {
                val private = bot.privateChannelCache.remove(channelId) ?:
                              bot.untrackedPrivateChannels.remove(channelId) ?:
                              return logUnCachedEntity("CHANNEL_DELETE", "private channel", channelId)

                if(private.recipient.untracked) {
                    bot.untrackedUsers.remove(private.recipient.id)
                }

                private.recipient.privateChannel = null

                // TODO Event
            }

            else -> DiscordWebSocket.Log.warn("Received a CHANNEL_DELETE for an unhandled channel type: ${channel.type}!")
        }
    }
}
