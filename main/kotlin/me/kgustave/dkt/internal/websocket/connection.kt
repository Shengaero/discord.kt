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
@file:Suppress("OverridingDeprecatedMember", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt.internal.websocket

import me.kgustave.dkt.DiscordBot

data class WebSocketConnection(val webSocket: DiscordWebSocket, val reconnect: Boolean) {
    val bot get() = webSocket.bot

    internal suspend fun run(last: Boolean) {
        if(webSocket.isShutdown) return
        if(!reconnect) {
            webSocket.connect()
        } else {
            webSocket.reconnect()
        }
        if(last) return
        try {
            bot.await(DiscordBot.Status.AWAITING_LOGIN_CONFIRMATION)
        } catch(e: IllegalStateException) {
            webSocket.close(1000, "")
            DiscordWebSocket.Log.debug("Shutdown while trying to make connection!")
        }
    }
}
