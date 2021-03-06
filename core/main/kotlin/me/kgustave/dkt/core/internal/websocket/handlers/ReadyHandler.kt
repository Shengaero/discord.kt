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

import me.kgustave.dkt.core.internal.data.events.RawReadyEvent
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.websocket.Payload

internal class ReadyHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val entities = bot.entities
        val ready = payload.d as RawReadyEvent

        val guilds = ready.guilds

        entities.handleSelfUser(ready.user)

        if(bot.guildSetupManager.setToCompleteAndIsReady(guilds.size)) {
            for(guild in guilds) bot.guildSetupManager.ready(guild)
        }

        for(chan in ready.privateChannels) {
            entities.handlePrivateChannel(chan)
        }
    }
}
