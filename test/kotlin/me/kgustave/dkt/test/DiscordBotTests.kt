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
import kotlinx.coroutines.runBlocking
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.DiscordBot.Status.SHUTDOWN
import me.kgustave.dkt.DiscordBot.Status.SHUTTING_DOWN
import me.kgustave.dkt.activity
import me.kgustave.dkt.compression
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.test.extensions.EnabledIfResourcePresent
import me.kgustave.dkt.test.tags.Slow
import me.kgustave.dkt.test.tags.UsesAPI
import me.kgustave.dkt.token
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Slow
@UsesAPI
@EnabledIfResourcePresent("/test-config.json")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscordBotTests: CoroutineTestBase() {
    private lateinit var bot: DiscordBot
    private val config = loadConfig()

    @Test fun `Test Bot Connection And Shutdown`() = runTest {
        bot = DiscordBot {
            token { config.token }
            compression { true }
        }

        bot.awaitReady()

        delay(2000)

        bot.shutdown()
    }

    @Test fun `Test Bot Update Status`() = runTest {
        bot = DiscordBot {
            token { config.token }
            compression { true }
            activity { playing("with discord.kt") }
        }

        bot.awaitReady()

        var presence = bot.presence
        var activity = presence.activity

        assertNotNull(activity)
        assertEquals(OnlineStatus.ONLINE, presence.status)
        assertEquals("with discord.kt", activity.name)
        assertEquals(Activity.Type.GAME, activity.type)

        delay(2000)

        bot.updatePresence {
            this.status = OnlineStatus.DO_NOT_DISTURB
            this.activity = listeningTo("the Gateway")
        }

        presence = bot.presence
        activity = presence.activity

        assertNotNull(activity)
        assertEquals(OnlineStatus.DO_NOT_DISTURB, presence.status)
        assertEquals("the Gateway", activity.name)
        assertEquals(Activity.Type.LISTENING, activity.type)
    }

    @AfterEach fun `Backoff Between Tests And Shutdown Bot If Necessary`() = runBlocking {
        if(!::bot.isInitialized) {
            if(bot.status != SHUTTING_DOWN && bot.status != SHUTDOWN) {
                bot.shutdown()
            }
        }

        // Do a 5 second backoff inbetween all tests
        delay(5000)
    }
}
