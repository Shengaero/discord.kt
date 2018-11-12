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

import me.kgustave.dkt.internal.data.RawGuildData
import me.kgustave.dkt.internal.entities.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.Payload

internal class GuildCreateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val raw = requireNotNull(payload.d as RawGuildData)
        val impl = bot.guildCache[raw.id] ?: return bot.guildSetupManager.create(raw)

        // the guild has gone unavailable!
        if(!impl.unavailable && raw.unavailable) {
            impl.unavailable = raw.unavailable
            // TODO Event
        }
        // the guild has just become available!
        else if(impl.unavailable && !raw.unavailable) {
            impl.unavailable = raw.unavailable
            // TODO Event
        }
    }
}
