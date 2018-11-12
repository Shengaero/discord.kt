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

import me.kgustave.dkt.core.internal.data.events.RawTypingStartEvent
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.entities.PrivateChannelImpl
import me.kgustave.dkt.core.internal.websocket.Payload
import java.time.Instant
import java.time.ZoneOffset

internal class TypingStartHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val (channelId, guildId, userId, timestamp) = payload.d as RawTypingStartEvent

        if(guildId != null) {
            if(bot.guildSetupManager.isSettingUp(guildId)) {
                return bot.guildSetupManager.cache(guildId, payload)
            }
        }

        // Do not cache typing events past here, they are too
        //frequent and during setup this will bloat the event cache

        val channel = bot.textChannelCache[channelId] ?: bot.privateChannelCache[channelId] ?:
                      bot.untrackedPrivateChannels[channelId] ?: return
        val user = (channel as? PrivateChannelImpl)?.recipient ?: bot.userCache[userId] ?: return
        val time = Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC)

        // TODO Event
    }
}
