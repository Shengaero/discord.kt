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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import me.kgustave.dkt.*
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.events.ShutdownEvent
import me.kgustave.dkt.handle.EventManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscordBotTests: CoroutineTestBase() {
    private val config = loadConfig()

    @Test fun `Test Bot Status Updating`() = runTest {
        val withDiscordKt = playing("with Discord.kt")
        val shutdown = CompletableDeferred<ShutdownEvent>()

        val bot = DiscordBot {
            token { config.token }
            compression { true }
            activity { withDiscordKt }
            status { OnlineStatus.DND }
            eventManager {
                object: EventManager {
                    override val listeners = emptyList<Any>()
                    override fun addListener(listener: Any) {}
                    override fun removeListener(listener: Any) {}
                    override suspend fun dispatch(event: Event) {
                        when(event) {
                            is ShutdownEvent -> shutdown.complete(event)
                        }
                    }
                }
            }
        }

        withTimeout(5000) { bot.connect() }

        assertEquals(withDiscordKt, bot.presence.activity)
        assertEquals(OnlineStatus.DND, bot.presence.status)

        delay(5 * 1000)

        val toTheGateway = listeningTo("the Gateway")

        bot.updatePresence {
            status { OnlineStatus.ONLINE }
            activity { toTheGateway }
        }

        assertEquals(toTheGateway, bot.presence.activity)
        assertEquals(OnlineStatus.ONLINE, bot.presence.status)

        delay(5 * 1000)

        bot.shutdown()

        val event = withTimeout(5000) { shutdown.await() }

        assertEquals(1000, event.code)
    }
}
