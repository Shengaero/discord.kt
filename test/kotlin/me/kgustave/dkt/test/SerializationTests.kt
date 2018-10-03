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

import kotlinx.serialization.json.JSON
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SerializationTests {
    @Test fun `Test Deserialize GatewayInfo`() {
        val gateway = JSON.parse<GatewayInfo>("""
            {
                "url": "wss://gateway.discord.gg/",
                "shards": 9,
                "session_start_limit": {
                    "total": 1000,
                    "remaining": 999,
                    "reset_after": 14400000
                }
            }
        """.trimIndent())

        assertEquals("wss://gateway.discord.gg/", gateway.url)
        assertEquals(9, gateway.shards)
        assertEquals(999, gateway.sessionStartLimit.remaining)
        assertEquals(1000, gateway.sessionStartLimit.total)
        assertEquals(14400000, gateway.sessionStartLimit.resetAfter)
    }
}
