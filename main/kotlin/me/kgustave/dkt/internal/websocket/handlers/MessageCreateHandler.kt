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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import me.kgustave.dkt.entities.Message
import me.kgustave.dkt.events.message.MessageReceivedEvent
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.internal.websocket.Payload

internal class MessageCreateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val data = payload.d as JsonObject
        val type = Message.Type.of(data["type"].int)

        if(type == Message.Type.UNKNOWN) {
            return DiscordWebSocket.Log.debug("Received an unknown message type (Type: $type)")
        }

        runCatching { bot.entities.handleReceivedMessage(data) }.onFailure {
            // TODO
        }.onSuccess { message ->
            bot.eventManager.dispatch(MessageReceivedEvent(bot, message))
        }
    }
}
