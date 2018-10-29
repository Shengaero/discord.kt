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
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.compression
import me.kgustave.dkt.entities.Activity
import me.kgustave.dkt.entities.OnlineStatus
import me.kgustave.dkt.entities.listeningTo
import me.kgustave.dkt.events.message.MessageReceivedEvent
import me.kgustave.dkt.handle.ExperimentalEventListeners
import me.kgustave.dkt.handle.on
import me.kgustave.dkt.handle.receive
import me.kgustave.dkt.test.extensions.EnabledIfResourcePresent
import me.kgustave.dkt.test.tags.Slow
import me.kgustave.dkt.test.tags.UsesAPI
import me.kgustave.dkt.token
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Slow
@UsesAPI
@Disabled("broken, will fix later :P")
@EnabledIfResourcePresent(TestConfigRes)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscordBotTests: CoroutineTestBase() {
    private val config = loadConfig()

    @Test fun `Test Bot Connection And Shutdown`() = runTest {
        val bot = DiscordBot {
            token { config.token }
            compression { true }
        }

        bot.awaitReady()

        delay(2000)

        bot.shutdown()

        // backoff
        delay(5000)
    }

    @Test fun `Test Bot Update Status`() = runBotTest { bot ->
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

    @EnabledIfResourcePresent(TestGuildConfigRes)
    @Test fun `Test Initial Guild Setup`() = runBotTest { bot ->
        val (id, name, member) = loadGuildConfig()

        val g = assertNotNull(bot.guildCache[id])
        assertEquals(name, g.name)

        val m = assertNotNull(g.memberCache[member.id])
        assertEquals(member.name, m.user.name)
        assertEquals(member.discriminator, m.user.discriminator)
    }

    @ExperimentalEventListeners
    @EnabledIfResourcePresent(TestGuildConfigRes)
    @Test fun `Test Message Events`() = runBotTest { bot ->
        val (id, name, member, channel) = loadGuildConfig()

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

    @ExperimentalEventListeners
    @EnabledIfResourcePresent(TestGuildConfigRes)
    @Test fun `Test Message File Upload`() = runBotTest { bot ->
        val (id, name, member, channel) = loadGuildConfig()

        val user = bot.userCache[member.id]
        val text = bot.textChannelCache[channel.id]

        assertNotNull(user)
        assertNotNull(text)

        val file = File("success.txt")
        if(file.exists()) file.delete()
        file.createNewFile()

        val number = Random.nextInt(0, 100).toString()

        file.writeText(number)

        val prompt = text.send("${user.mention} Send the code to pass the test")
            .appendFile(file.inputStream(), "code.txt")
            .await()

        val event = bot.receive<MessageReceivedEvent> event@ {
            val message = it.message
            return@event message.author.id == member.id &&
                         message.guild?.id == id &&
                         message.guild?.name == name &&
                         (message.content == number || message.content == "fail")
        }

        assertTrue(file.delete())
        prompt.delete().await()
        event.message.delete().await()
    }

    @ExperimentalEventListeners
    @EnabledIfResourcePresent(TestGuildConfigRes)
    @Test fun `Test EventListener Dsl`() = runBotTest { bot ->
        val (id, _, member, channel) = loadGuildConfig()

        val done = CompletableDeferred<Unit>()

        bot.on<MessageReceivedEvent> { event ->
            if(event.message.guild?.id != id) return@on
            if(event.message.member?.user?.id == member.id) return@on
            if(event.message.channel.id != channel.id) return@on

            if(event.message.content == "done") {
                event.channel.send("Done").promise({ message ->
                    Thread.sleep(3000)
                    event.message.delete().promise()
                    message.delete().promise()
                    done.complete(Unit)
                }, { done.cancel(it) })
            }
        }
    }

    private fun runBotTest(block: suspend (bot: DiscordBot) -> Unit) = runTest {
        val bot = DiscordBot {
            token { config.token }
            compression { true }
        }

        bot.awaitReady()

        block(bot)

        bot.shutdown()

        delay(5000)
    }
}
