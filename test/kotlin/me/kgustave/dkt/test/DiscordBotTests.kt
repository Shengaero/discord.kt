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
import kotlinx.coroutines.runBlocking
import me.kgustave.dkt.*
import me.kgustave.dkt.DiscordBot.Status.SHUTDOWN
import me.kgustave.dkt.DiscordBot.Status.SHUTTING_DOWN
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.events.ShutdownEvent
import me.kgustave.dkt.events.message.MessageReceivedEvent
import me.kgustave.dkt.handle.EventManager
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.test.extensions.EnabledIfResourcePresent
import me.kgustave.dkt.test.tags.Slow
import me.kgustave.dkt.test.tags.UsesAPI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @EnabledIfResourcePresent("/test-guild-config.json")
    @Test fun `Test Initial Guild Setup`() = runTest {
        val (id, name, member) = loadGuildConfig()

        bot = DiscordBot {
            token { config.token }
            compression { true }
        }

        bot.awaitReady()

        val g = assertNotNull(bot.guildCache[id])
        assertEquals(name, g.name)

        val m = assertNotNull(g.memberCache[member.id])
        assertEquals(member.name, m.user.name)
        assertEquals(member.discriminator, m.user.discriminator)
    }

    @EnabledIfResourcePresent("/test-guild-config.json")
    @Test fun `Test Message Events`() = runTest {
        val (id, name, member, channel) = loadGuildConfig()

        bot = DiscordBot {
            token { config.token }
            compression { true }
            eventManager { AwaitingEventManager() }
        }

        bot.awaitReady()

        val user = bot.userCache[member.id]
        val text = bot.textChannelCache[channel.id]

        assertNotNull(user)
        assertNotNull(text)

        val prompt = text.send("${user.mention} please type 'pass' to pass this test").await()

        val event = bot.receive<MessageReceivedEvent> event@ {
            val message = it.message
            return@event message.author.id == member.id &&
                         message.guild?.id == id &&
                         message.guild?.name == name &&
                         (message.content == "pass" || message.content == "fail")
        }

        assertEquals("pass", event.message.content)

        prompt.delete().await()
        event.message.delete().await()
    }

    @Disabled
    @EnabledIfResourcePresent("/test-guild-config.json")
    @Test fun `Test Message File Upload`() = runTest {
        val (id, name, member, channel) = loadGuildConfig()

        bot = DiscordBot {
            token { config.token }
            compression { true }
            eventManager { AwaitingEventManager() }
        }

        bot.awaitReady()

        val user = bot.userCache[member.id]
        val text = bot.textChannelCache[channel.id]

        assertNotNull(user)
        assertNotNull(text)

        val file = File("success.txt")
        assertTrue(file.createNewFile())

        val number = Random.nextInt(0, 100).toString()

        file.writeText(number)

        val prompt = text.send("Send the code to pass the test").addFile("code.txt", file.inputStream()).await()
        val event = bot.receive<MessageReceivedEvent> event@ {
            val message = it.message
            return@event message.author.id == member.id &&
                         message.guild?.id == id &&
                         message.guild?.name == name &&
                         (message.content == number || message.content == "fail")
        }

        file.delete()
        prompt.delete().await()
        event.message.delete().await()
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

    class AwaitingEventManager: EventManager {
        private val handlers = HashMap<KClass<*>, MutableSet<Awaiting>>()

        override val listeners: Collection<Any> get() = handlers.values

        override fun dispatch(event: Event) {
            if(event is ShutdownEvent) {
                handlers.values.forEach { set ->
                    set.forEach { awaiting ->
                        awaiting.completion.cancel()
                    }
                }
                return
            }

            var klass = event::class

            @Suppress("UNCHECKED_CAST")
            while(klass.isSubclassOf(Event::class)) {
                val set = handlers[klass]
                val toProcess = set?.toList()

                toProcess?.forEach { awaiting ->
                    if(awaiting.condition(event)) {
                        awaiting.completion.complete(event)
                        set -= awaiting
                    }
                }

                val superclass = klass.superclasses.firstOrNull { it.isSubclassOf(Event::class) } ?: break
                klass = superclass as KClass<out Event>
            }
        }

        override fun addListener(listener: Any) {
            if(listener is Awaiting) {
                val set = handlers.computeIfAbsent(listener.type) { mutableSetOf() }
                set += listener
            }
        }

        override fun removeListener(listener: Any) {
            if(listener is Awaiting) {
                val set = handlers[listener.type] ?: return
                set -= listener
                listener.completion.cancel()
            }
        }

        data class Awaiting(val type: KClass<*>, val completion: CompletableDeferred<Event>,
                            val condition: (Event) -> Boolean = { true })
    }

    companion object {
        suspend inline fun <reified E: Event> DiscordBot.receive(
            crossinline condition: (event: E) -> Boolean = { true }): E {
            this as DiscordBotImpl
            val manager = this.eventManager
            require(manager is AwaitingEventManager)
            val completion = CompletableDeferred<Event>()
            val awaiting = AwaitingEventManager.Awaiting(E::class, completion) { event ->
                if(event !is E) return@Awaiting false
                return@Awaiting condition(event)
            }
            manager.addListener(awaiting)
            return completion.await() as E
        }
    }
}
