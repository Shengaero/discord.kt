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

import me.kgustave.dkt.core.entities.Channel
import me.kgustave.dkt.core.internal.data.RawChannel
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.websocket.Payload

internal class ChannelCreateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    @Suppress("UNUSED_VARIABLE")
    override fun handle(payload: Payload) {
        val channel = payload.d as RawChannel
        val guildId = channel.guildId

        if(guildId != null) {
            if(bot.guildSetupManager.isSettingUp(guildId)) {
                return bot.guildSetupManager.cache(guildId, payload)
            }
        }

        when(val type = Channel.Type.of(channel.type)) {
            Channel.Type.TEXT -> {
                val chan = bot.entities.handleTextChannel(channel, guildId!!)
                // TODO Event
            }

            Channel.Type.VOICE -> {
                val chan = bot.entities.handleVoiceChannel(channel, guildId!!)
                // TODO Event
            }

            Channel.Type.CATEGORY -> {
                val chan = bot.entities.handleCategory(channel, guildId!!)
                // TODO Event
            }

            Channel.Type.PRIVATE -> {
                val chan = bot.entities.handlePrivateChannel(channel)
                // TODO Event
            }

            Channel.Type.UNKNOWN -> {
                // TODO Logging(?)
            }
        }
    }
}
