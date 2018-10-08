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
package me.kgustave.dkt.internal.websocket

import me.kgustave.dkt.DiscordBot

sealed class WebSocketConnection {
    abstract val reconnect: Boolean
    abstract val bot: DiscordBot
    open val shardInfo: DiscordBot.ShardInfo? get() = bot.shardInfo

    abstract suspend fun run(last: Boolean)
}

class InitialWebSocketConnection
internal constructor(private val webSocket: DiscordWebSocket): WebSocketConnection() {
    override val bot: DiscordBot get() = webSocket.bot
    override val reconnect: Boolean get() = false
    override suspend fun run(last: Boolean) {
        if(webSocket.isShutdown) return
        webSocket.connect()
    }
}

class ReconnectWebSocketConnection
internal constructor(private val webSocket: DiscordWebSocket): WebSocketConnection() {
    override val reconnect: Boolean get() = true
    override val bot: DiscordBot get() = webSocket.bot
    override suspend fun run(last: Boolean) {
        if(webSocket.isShutdown) return
        webSocket.reconnect()
    }
}
