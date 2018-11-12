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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import me.kgustave.dkt.core.entities.Message
import me.kgustave.dkt.core.events.message.MessageReceivedEvent
import me.kgustave.dkt.core.internal.cache.EventCache
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.entities.EntityHandler
import me.kgustave.dkt.core.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.core.internal.websocket.Payload
import me.kgustave.dkt.core.internal.util.snowflake

internal class MessageCreateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val data = payload.d as JsonObject
        val type = Message.Type.of(data["type"].int)

        if(type == Message.Type.UNKNOWN) {
            return DiscordWebSocket.Log.debug("Received an unknown message type (Type: $type)")
        }

        if("guild_id" in data) {
            val guildId = data["guild_id"].snowflake
            if(bot.guildSetupManager.isSettingUp(guildId)) {
                return bot.guildSetupManager.cache(guildId, payload)
            }
        }

        val result = runCatching { bot.entities.handleReceivedMessage(data, true) }.onFailure {
            if(it is IllegalStateException) when(it.message) {
                EntityHandler.MissingChannelError -> {
                    val channelId = data["channel_id"].snowflake
                    bot.eventCache.cache(EventCache.Type.CHANNEL, channelId, payload, this::handle)
                    return logUnCachedEntity("MESSAGE_CREATE", "message channel", channelId)
                }

                EntityHandler.MissingUserError -> {
                    val userId = data["user_id"].snowflake
                    bot.eventCache.cache(EventCache.Type.USER, userId, payload, this::handle)
                    return logUnCachedEntity("MESSAGE_CREATE", "user", userId)
                }
            }
        }

        val message = result.getOrThrow()

        bot.emit(MessageReceivedEvent(bot, message))
    }
}
