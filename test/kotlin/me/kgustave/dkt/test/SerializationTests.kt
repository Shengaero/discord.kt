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
@file:Suppress("unused")

package me.kgustave.dkt.test

import kotlinx.serialization.parse
import me.kgustave.dkt.internal.data.RawEmoji
import me.kgustave.dkt.internal.data.RawRole
import me.kgustave.dkt.internal.data.RawUser
import me.kgustave.dkt.internal.data.RawVoiceState
import me.kgustave.dkt.internal.data.responses.GatewayInfo
import me.kgustave.dkt.util.JsonParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerializationTests {
    @Test fun `Test Deserialize GatewayInfo`() {
        val gateway = JsonParser.parse<GatewayInfo>("""
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

    @Test fun `Test Deserialize RawUser`() {
        val user = JsonParser.parse<RawUser>("""
            {
                "id": "80351110224678912",
                "username": "Nelly",
                "discriminator": "1337",
                "avatar": "8342729096ea3675442027381ff50dfe"
            }
        """.trimIndent())

        assertEquals(80351110224678912L, user.id)
        assertEquals("Nelly", user.username)
        assertEquals("1337", user.discriminator)
        assertEquals("8342729096ea3675442027381ff50dfe", user.avatar)
        assertFalse(user.bot)
    }

    @Test fun `Test Deserialize RawRole`() {
        val role = JsonParser.parse<RawRole>("""
            {
                "id": "41771983423143936",
                "name": "WE DEM BOYZZ!!!!!!",
                "color": 3447003,
                "hoist": true,
                "position": 1,
                "permissions": 66321471,
                "managed": false,
                "mentionable": false
            }
        """.trimIndent())

        assertEquals(41771983423143936L, role.id)
        assertEquals("WE DEM BOYZZ!!!!!!", role.name)
        assertEquals(3447003, role.color)
        assertTrue(role.hoist)
        assertEquals(1, role.position)
        assertEquals(66321471, role.permissions)
        assertFalse(role.managed)
        assertFalse(role.mentionable)
    }

    @Test fun `Test Deserialize RawEmoji`() {
        val emoji = JsonParser.parse<RawEmoji>("""
            {
              "id": "41771983429993937",
              "name": "LUL",
              "roles": [ "41771983429993000", "41771983429993111" ],
              "user": {
                "username": "Luigi",
                "discriminator": "0002",
                "id": "96008815106887111",
                "avatar": "5500909a3274e1812beb4e8de6631111"
              },
              "require_colons": true,
              "managed": false,
              "animated": false
            }
        """.trimIndent())

        assertEquals(41771983429993937L, emoji.id)
        assertEquals("LUL", emoji.name)
        assertEquals(2, emoji.roles.size)
        assertEquals(41771983429993000L, emoji.roles[0])
        assertEquals(41771983429993111L, emoji.roles[1])
        assertEquals(RawUser(
            id = 96008815106887111L,
            username = "Luigi",
            discriminator = "0002",
            avatar = "5500909a3274e1812beb4e8de6631111"
        ), emoji.user)
        assertTrue(emoji.requireColons)
        assertFalse(emoji.managed)
        assertFalse(emoji.animated)
    }

    @Test fun `Test Deserialize RawEmoji (Reaction)`() {
        val emojiA = JsonParser.parse<RawEmoji>("""
            {
              "id": null,
              "name": "🔥"
            }
        """.trimIndent())

        assertNull(emojiA.id)
        assertEquals("🔥", emojiA.name)

        val emojiB = JsonParser.parse<RawEmoji>("""
            {
              "id": "41771983429993937",
              "name": "LUL"
            }
        """.trimIndent())

        assertEquals(41771983429993937L, emojiB.id)
        assertEquals("LUL", emojiB.name)
    }

    @Test fun `Test Deserialize RawVoiceState`() {
        val voiceState = JsonParser.parse<RawVoiceState>("""
            {
                "channel_id": "157733188964188161",
                "user_id": "80351110224678912",
                "session_id": "90326bd25d71d39b9ef95b299e3872ff",
                "deaf": false,
                "mute": false,
                "self_deaf": false,
                "self_mute": true,
                "suppress": false
            }
        """.trimIndent())

        assertEquals(157733188964188161L, voiceState.channelId)
        assertEquals(80351110224678912L, voiceState.userId)
        assertEquals("90326bd25d71d39b9ef95b299e3872ff", voiceState.sessionId)
        assertFalse(voiceState.deaf)
        assertFalse(voiceState.mute)
        assertFalse(voiceState.selfDeaf)
        assertTrue(voiceState.selfMute)
        assertFalse(voiceState.suppress)
    }

    // These are used for serialization test with json content
    //that is a bit too long (IE: a guild json from ready event)
    //
    // When used, make sure to label the test with a proper
    //@EnableIfResourcePresent annotation!
    private companion object {
        private const val ResDir = "/serialization"

        @JvmStatic private fun loadJsonString(name: String): String {
            val resource = this::class.java.getResource("$ResDir/$name")
            return resource.readText(Charsets.UTF_8)
        }
    }
}
