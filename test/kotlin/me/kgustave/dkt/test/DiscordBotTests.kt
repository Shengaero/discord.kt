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

import kotlinx.coroutines.delay
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.compression
import me.kgustave.dkt.token
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscordBotTests: CoroutineTestBase() {
    private val config = loadConfig()

    @Test fun `Test Bot Connection And Shutdown`() = runTest {
        val bot = DiscordBot {
            token { config.token }
            compression { true }
        }

        bot.awaitReady()
        delay(10 * 1000)
        bot.shutdown()
        delay(5*1000)
    }
}
