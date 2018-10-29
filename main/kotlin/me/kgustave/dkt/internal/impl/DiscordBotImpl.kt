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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt.internal.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.whileSelect
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.Presence
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.handle.ExperimentalEventListeners
import me.kgustave.dkt.http.engine.OkHttp
import me.kgustave.dkt.http.engine.websockets.WebSockets
import me.kgustave.dkt.internal.cache.EventCache
import me.kgustave.dkt.internal.cache.SnowflakeCacheImpl
import me.kgustave.dkt.internal.data.RawUser
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.internal.websocket.guild.GuildSetupManager
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.promises.emptyPromise
import me.kgustave.dkt.promises.restPromise
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.serialization.DiscordSerializer
import kotlin.concurrent.thread

internal class DiscordBotImpl(config: DiscordBot.Config): DiscordBot {
    override val token = config.token
    override val sessionHandler = config.sessionHandler

    @ExperimentalEventListeners
    override val eventManager = config.eventManager

    override lateinit var self: SelfUserImpl
    override var presence = PresenceImpl(config.presence)
    override var responses = 0L
    override var status = DiscordBot.Status.INITIALIZING
        internal set(value) = synchronized(field) {
            //val old = field
            field = value
        }

    val untrackedUsers = hashMapOf<Long, UserImpl>()
    val untrackedPrivateChannels = hashMapOf<Long, PrivateChannelImpl>()

    override val userCache = SnowflakeCacheImpl(UserImpl::name)
    override val guildCache = SnowflakeCacheImpl(GuildImpl::name)
    override val textChannelCache = SnowflakeCacheImpl(TextChannelImpl::name)
    override val voiceChannelCache = SnowflakeCacheImpl(VoiceChannelImpl::name)
    override val categoryCache = SnowflakeCacheImpl(CategoryImpl::name)
    override val privateChannelCache = SnowflakeCacheImpl<PrivateChannelImpl>()

    private lateinit var shutdownHook: Thread

    val entities = EntityHandler(this)
    val dispatcherProvider = config.dispatcherProvider

    //////////////////////
    // HTTP CLIENT INIT //
    //////////////////////

    val httpClient = HttpClient(OkHttp) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = DiscordSerializer()
        }

        engine {
            expectSuccess = false
            pipelining = true
            response.defaultCharset = Charsets.UTF_8
        }
    }

    ////////////////////
    // REQUESTER INIT //
    ////////////////////

    val rateLimitDispatcher: CoroutineDispatcher
    val shutdownRateLimitDispatcher: Boolean
    val requester: Requester

    init {
        val (dispatcher, shutdownAutomatically) = dispatcherProvider.provideRateLimitDispatcher(null) // FIXME

        this.rateLimitDispatcher = dispatcher
        this.shutdownRateLimitDispatcher = shutdownAutomatically
        this.requester = Requester(
            client = httpClient,
            token = token,
            rateLimitDispatcher = rateLimitDispatcher,
            shutdownDispatcher = shutdownRateLimitDispatcher,
            globalRateLimitProvider = sessionHandler
        )
    }

    /////////////////////////////
    // PROMISE DISPATCHER INIT //
    /////////////////////////////

    val promiseDispatcher: CoroutineDispatcher
    val shutdownPromiseDispatcher: Boolean

    init {
        val (dispatcher, shutdownAutomatically) = dispatcherProvider.providePromiseDispatcher(null) // FIXME

        this.promiseDispatcher = dispatcher
        this.shutdownPromiseDispatcher = shutdownAutomatically
    }

    ///////////////
    // WEBSOCKET //
    ///////////////

    val maxReconnectDelay = 900 // FIXME Make configurable
    val eventCache = EventCache()
    val guildSetupManager = GuildSetupManager(this)
    val websocket = DiscordWebSocket(this, config.compression)

    init {
        if(config.startAutomatically) connect()
    }

    override fun connect(): DiscordBot {
        val shutdownHook = thread(
            start = false,
            name = "Discord.kt Shutdown",
            isDaemon = true,
            block = { runBlocking { shutdown() } }
        )
        this.shutdownHook = shutdownHook
        Runtime.getRuntime().addShutdownHook(shutdownHook)
        websocket.init()
        return this
    }

    override fun updatePresence(block: Presence.Builder.() -> Unit) {
        val builder = Presence.Builder(presence).apply(block)
        this.presence = presence.copy(
            status = builder.status,
            afk = builder.afk,
            activity = builder.activity
        )
        this.websocket.updatePresence()
    }

    override fun lookupUserById(id: Long): RestPromise<User> {
        userCache[id]?.let { return emptyPromise(it) }

        return restPromise(Route.GetUser.format(id)) { call ->
            val user = call.response.receive<RawUser>()
            return@restPromise entities.handleUntrackedUser(user, false)
        }
    }

    override fun createGuild(): RestPromise<Guild> {
        check(!status.isInit) { "Cannot create guild while initializing!" }
        check(guildCache.size < 10) { "Cannot create guild while on more than 10 guilds already!" }

        TODO("Not yet implemented!")
    }

    @ExperimentalEventListeners
    override fun addListener(listener: Any) = eventManager.addListener(listener)

    @ExperimentalEventListeners
    override fun removeListener(listener: Any) = eventManager.removeListener(listener)

    override suspend fun await(status: DiscordBot.Status): DiscordBot {
        require(status.isInit) { "Cannot await non-init status: $status" }
        if(this.status.ordinal >= status.ordinal) return this
        whileSelect { onTimeout(50) { this@DiscordBotImpl.status.ordinal < status.ordinal } }
        return this
    }

    override suspend fun shutdown() {
        if(status == DiscordBot.Status.SHUTTING_DOWN ||
           status == DiscordBot.Status.SHUTDOWN) return

        status = DiscordBot.Status.SHUTTING_DOWN

        httpClient.close()
        websocket.shutdown()
        shutdownInternally()
        websocket.freeDispatchers()
    }

    internal fun emit(event: Event) {
        eventManager.dispatch(event)
    }

    internal fun selfIsInit() = ::self.isInitialized

    internal fun shutdownInternally() {
        if(status == DiscordBot.Status.SHUTDOWN) return
        requester.shutdown()
        if(shutdownPromiseDispatcher && promiseDispatcher is AutoCloseable) {
            promiseDispatcher.close()
        }

        if(::shutdownHook.isInitialized) runCatching {
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }

        status = DiscordBot.Status.SHUTDOWN
    }

    internal suspend fun getGatewayInfo(): GatewayInfo {
        val promise = restPromise(Route.GetGatewayBot) { call -> call.response.receive<GatewayInfo>() }

        return promise.await()
    }
}
