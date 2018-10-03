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
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.WebSockets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.entities.SelfUser
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.requests.serialization.DiscordSerializer
import me.kgustave.dkt.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.requests.RestTask
import me.kgustave.dkt.requests.Requester
import kotlin.concurrent.thread

internal class DiscordBotImpl(config: DiscordBot.Config): DiscordBot {
    override val token = config.token
    override var responses = 0L
    override val sessionHandler = config.sessionHandler
    override val shardInfo = config.shardInfo

    override lateinit var self: SelfUser
    lateinit var gatewayUrl: String

    //////////////////////
    // HTTP CLIENT INIT //
    //////////////////////

    val httpClient = HttpClient(OkHttp) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = DiscordSerializer()
        }
    }

    ////////////////////
    // REQUESTER INIT //
    ////////////////////

    val rateLimitDispatcher: CoroutineDispatcher
    val shutdownRateLimitDispatcher: Boolean
    val requester: Requester

    init {
        val (dispatcher, shutdownAutomatically) = sessionHandler.dispatcherProvider
            .provideRateLimitDispatcher(null) // FIXME

        this.rateLimitDispatcher = dispatcher
        this.shutdownRateLimitDispatcher = shutdownAutomatically
        this.requester = Requester(
            client = httpClient,
            token = token,
            rateLimitDispatcher = rateLimitDispatcher,
            shutdownDispatcher = shutdownRateLimitDispatcher,
            sessionHandler = sessionHandler
        )
    }

    /////////////////////////////
    // PROMISE DISPATCHER INIT //
    /////////////////////////////

    val promiseDispatcher: CoroutineDispatcher
    val shutdownPromiseDispatcher: Boolean

    init {
        val (dispatcher, shutdownAutomatically) = sessionHandler.dispatcherProvider
            .providePromiseDispatcher(null) // FIXME

        this.promiseDispatcher = dispatcher
        this.shutdownPromiseDispatcher = shutdownAutomatically
    }

    val entities = EntityHandler(this)
    val websocket = DiscordWebSocket(token, requester, config.useCompression)

    // property checkers
    fun gatewayUrlIsInit(): Boolean = ::gatewayUrl.isInitialized
    fun selfIsInit(): Boolean = ::self.isInitialized

    override suspend fun connect() {
        websocket.connect()
        Runtime.getRuntime().addShutdownHook(thread(
            start = false,
            name = "Discord.kt Shutdown",
            isDaemon = true,
            block = this::shutdown
        ))
    }

    override fun lookupUserById(id: Long): RestTask<User> {
        TODO("lookupUserById impl")
    }

    override fun shutdown() {
        httpClient.close()
        // TODO Find a better way to handle this
        runBlocking {
            websocket.shutdown()
        }
    }
}
