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
package me.kgustave.dkt.core.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.parse
import me.kgustave.dkt.core.internal.util.JsonParser

const val TestConfigRes = "/test-config.json"
const val TestGuildConfigRes = "/test-guild-config.json"

@Serializable data class TestConfig(val token: String)

@Serializable data class GuildConfig(val id: Long, val name: String, val member: MemberConfig, val channel: ChannelConfig) {
    @Serializable data class ChannelConfig(val id: Long)
    @Serializable data class MemberConfig(val id: Long, val name: String, val discriminator: String)
}

private val instance by lazy {
    val resource = TestConfig::class.java.getResource("/test-config.json")
    return@lazy JsonParser.parse<TestConfig>(resource.readText())
}

private val guildInstance by lazy {
    val resource = GuildConfig::class.java.getResource("/test-guild-config.json")
    return@lazy JsonParser.parse<GuildConfig>(resource.readText())
}

fun loadConfig(): TestConfig = instance
fun loadGuildConfig(): GuildConfig = guildInstance
