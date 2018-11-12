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

import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.internal.websocket.EventType
import me.kgustave.dkt.internal.websocket.EventType.*
import me.kgustave.dkt.internal.websocket.Payload

internal abstract class WebSocketHandler(val bot: DiscordBotImpl) {
    abstract fun handle(payload: Payload)

    companion object {
        fun newFullSet(bot: DiscordBotImpl): Map<EventType, WebSocketHandler> = mapOf(
            READY to ReadyHandler(bot),
            RESUMED to ResumeHandler(bot),
            CHANNEL_CREATE to ChannelCreateHandler(bot),
            CHANNEL_UPDATE to ChannelUpdateHandler(bot),
            CHANNEL_DELETE to ChannelDeleteHandler(bot),
            GUILD_CREATE to GuildCreateHandler(bot),
            GUILD_DELETE to GuildDeleteHandler(bot),
            *GuildBanHandler(bot).let { arrayOf(GUILD_BAN_ADD to it, GUILD_BAN_REMOVE to it) },
            GUILD_MEMBER_ADD to GuildMemberAddHandler(bot),
            GUILD_MEMBER_UPDATE to GuildMemberUpdateHandler(bot),
            GUILD_MEMBER_REMOVE to GuildMemberRemoveHandler(bot),
            GUILD_MEMBERS_CHUNK to GuildMembersChunkHandler(bot),
            MESSAGE_CREATE to MessageCreateHandler(bot),
            TYPING_START to TypingStartHandler(bot)
        )

        fun logUnCachedEntity(t: String, entity: String, id: Long) {
            DiscordWebSocket.Log.debug("Received $t event containing data for a un-cached $entity (ID: $id)")
        }
    }
}
