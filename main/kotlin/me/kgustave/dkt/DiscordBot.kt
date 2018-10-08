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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.cache.NamedSnowflakeCache
import me.kgustave.dkt.entities.cache.SnowflakeCache
import me.kgustave.dkt.handle.SessionHandler
import me.kgustave.dkt.handle.SessionHandlerAdapter
import me.kgustave.dkt.requests.RestTask

/**
 * @author Kaidan Gustave
 */
interface DiscordBot {
    val token: String
    val responses: Long
    val self: SelfUser
    val sessionHandler: SessionHandler
    val shardInfo: ShardInfo?
    val status: DiscordBot.Status
    val presence: Presence

    val guildCache: NamedSnowflakeCache<Guild> get() = TODO()
    val userCache: NamedSnowflakeCache<User> get() = TODO()
    val textChannelCache: NamedSnowflakeCache<TextChannel> get() = TODO()
    val voiceChannelCache: NamedSnowflakeCache<VoiceChannel> get() = TODO()
    val privateChannelCache: SnowflakeCache<PrivateChannel> get() = TODO()

    suspend fun connect(): DiscordBot

    suspend fun await(status: DiscordBot.Status): DiscordBot

    fun updatePresence(block: Presence.Builder.() -> Unit)

    fun shutdown()

    fun lookupUserById(id: Long): RestTask<User>

    data class ShardInfo internal constructor(val id: Int, val total: Int)

    enum class Status(val isInit: Boolean = false) {
        INITIALIZING(true),
        INITIALIZED(true),
        LOGGING_IN(true),
        CONNECTING_TO_WEBSOCKET(true),
        IDENTIFYING_SESSION(true),
        AWAITING_LOGIN_CONFIRMATION(true),
        LOADING_SUBSYSTEMS(true),
        CONNECTED(true),
        DISCONNECTED,
        RECONNECT_QUEUED,
        WAITING_TO_RECONNECT,
        ATTEMPTING_TO_RECONNECT,
        SHUTTING_DOWN,
        SHUTDOWN,
        FAILED_TO_LOGIN
    }

    class Config {
        lateinit var token: String

        internal val presence = Presence.Builder()

        var autoLaunchTasks: Boolean = false
        var cacheEntities: Boolean = true
        var sessionHandler: SessionHandler = SessionHandlerAdapter()
        var shardInfo: ShardInfo? = null
        var useCompression: Boolean = false
        var activity: Activity?
            get() = presence.activity
            set(value) { presence.activity = value }
        var status: OnlineStatus
            get() = presence.status
            set(value) { presence.status = value }
        var afk: Boolean
            get() = presence.afk
            set(value) { presence.afk = value }

        @BotConfigDsl infix fun Int.of(total: Int): ShardInfo = ShardInfo(this, total)

        internal fun requireToken() {
            require(::token.isInitialized) { "Token not specified!" }
        }
    }
}
