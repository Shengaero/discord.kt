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
package me.kgustave.dkt.test

//import kotlinx.coroutines.newFixedThreadPoolContext
//import kotlinx.coroutines.runBlocking
//import me.kgustave.dkt.internal.websocket.DiscordWebSocket
//import me.kgustave.dkt.requests.Requester
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
//import kotlin.test.assertTrue

@Suppress("unused")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketTests: CoroutineTestBase() {
//    private val config = loadConfig()
//    private val client = defaultHttpClient()
//    private val dispatcher = newFixedThreadPoolContext(3, "WebSocketTests RateLimit Dispatcher")
//    private val requester = Requester(client, config.token, dispatcher, shutdownDispatcher = true)
//    private val websocket = DiscordWebSocket(config.token, requester, compression = true, shouldReconnect = false)
//
//    @BeforeAll fun setup() = runBlocking {
//        websocket.connect()
//    }
//
//    @Test fun `Test WebSocket Connection`() {
//        assertTrue(websocket.isConnected)
//    }
//
//    @AfterAll fun teardown() = runBlocking {
//        websocket.shutdown()
//    }
}
