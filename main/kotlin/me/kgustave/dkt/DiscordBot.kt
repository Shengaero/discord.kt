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
@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DeprecatedCallableAddReplaceWith", "DEPRECATION")
package me.kgustave.dkt

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.cache.SnowflakeCache
import me.kgustave.dkt.handle.*
import me.kgustave.dkt.internal.handle.EventManagerImpl
import me.kgustave.dkt.promises.RestPromise
import okhttp3.OkHttpClient

interface DiscordBot {
    val token: String
    val responses: Long
    val self: SelfUser
    val sessionHandler: SessionHandler
    val status: DiscordBot.Status
    val presence: Presence

    @ExperimentalEventListeners
    val eventManager: EventManager

    val userCache: SnowflakeCache<out User>
    val guildCache: SnowflakeCache<out Guild>
    val textChannelCache: SnowflakeCache<out TextChannel>
    val voiceChannelCache: SnowflakeCache<out VoiceChannel>
    val categoryCache: SnowflakeCache<out Category>
    val privateChannelCache: SnowflakeCache<out PrivateChannel>

    fun connect(): DiscordBot

    fun updatePresence(block: Presence.Builder.() -> Unit)

    fun lookupUserById(id: Long): RestPromise<User>

    fun createGuild(): RestPromise<Guild>

    @ExperimentalEventListeners
    fun addListener(listener: Any)

    @ExperimentalEventListeners
    fun removeListener(listener: Any)

    suspend fun await(status: DiscordBot.Status): DiscordBot

    suspend fun awaitReady(): DiscordBot = await(DiscordBot.Status.CONNECTED)

    suspend fun shutdown()

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

    open class Config {
        lateinit var token: String

        internal val presence = Presence.Builder()

        var startAutomatically: Boolean = true
        var autoLaunchTasks: Boolean = false
        var cacheEntities: Boolean = true

        @ExperimentalEventListeners
        var eventManager: EventManager = EventManagerImpl()

        var sessionHandler: SessionHandler = SessionHandlerAdapter()
        var dispatcherProvider: DispatcherProvider = DispatcherProvider.Default
        var compression: Boolean = false
        var activity: Activity?
            get() = presence.activity
            set(value) { presence.activity = value }
        var status: OnlineStatus
            get() = presence.status
            set(value) { presence.status = value }
        var afk: Boolean
            get() = presence.afk
            set(value) { presence.afk = value }

        private var okHttp: OkHttpClient.Builder.() -> Unit = {}

        @BotConfigDsl fun okHttp(okHttp: OkHttpClient.Builder.() -> Unit) {
            this.okHttp = okHttp
        }

        internal fun requireToken() {
            require(::token.isInitialized) { "Token not specified!" }
        }
    }
}
