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
@file:Suppress("unused")
package me.kgustave.dkt.internal.websocket

import me.kgustave.dkt.DiscordBot
import java.lang.IllegalStateException

sealed class WebSocketConnection(val webSocket: DiscordWebSocket, val reconnect: Boolean) {
    val bot get() = webSocket.bot

    abstract suspend fun connect()

    internal suspend fun run(last: Boolean) {
        if(webSocket.isShutdown) return
        connect()
        if(last) return
        try {
            bot.await(DiscordBot.Status.AWAITING_LOGIN_CONFIRMATION)
        } catch(e: IllegalStateException) {
            webSocket.close(1000, "")
            DiscordWebSocket.Log.debug("Shutdown while trying to make connection!")
        }
    }
}

class InitialWebSocketConnection
internal constructor(webSocket: DiscordWebSocket): WebSocketConnection(webSocket, false) {
    override suspend fun connect() = webSocket.connect()
}

class ReconnectWebSocketConnection
internal constructor(webSocket: DiscordWebSocket): WebSocketConnection(webSocket, true) {
    override suspend fun connect() = webSocket.reconnect()
}
