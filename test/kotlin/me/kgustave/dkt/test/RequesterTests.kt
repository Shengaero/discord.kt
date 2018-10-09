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

import io.ktor.client.call.receive
import kotlinx.coroutines.newFixedThreadPoolContext
import me.kgustave.dkt.handle.SessionHandlerAdapter
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import me.kgustave.dkt.requests.BasicDiscordResponse
import me.kgustave.dkt.requests.DiscordRequest
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.test.extensions.EnabledIfResourcePresent
import me.kgustave.dkt.test.tags.UsesAPI
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@UsesAPI
@EnabledIfResourcePresent("/test-config.json")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequesterTests: CoroutineTestBase() {
    private val httpClient = defaultHttpClient()
    private val config = loadConfig()
    private val rateLimitDispatcher = newFixedThreadPoolContext(3, "Requester Tests RateLimit Context")
    private val requester =
        Requester(httpClient, config.token, rateLimitDispatcher, false, SessionHandlerAdapter())

    @Test fun `Test Get Gateway`() = runTest {
        val response = requester.request(DiscordRequest(Route.GetGatewayBot))

        assertTrue(response is BasicDiscordResponse)

        val gateway = response.receive<GatewayInfo>()

        assertTrue(gateway.url.startsWith("wss://"))
        assertEquals(expected = 1, actual = gateway.shards)
        assertTrue(gateway.sessionStartLimit.remaining <= gateway.sessionStartLimit.total)
    }

    @AfterAll fun destroy() {
        requester.shutdown()
        rateLimitDispatcher.close()
    }
}
