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
@file:JvmName("Discord")
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "FunctionName")
package me.kgustave.dkt

import me.kgustave.dkt.handle.SessionHandler
import me.kgustave.dkt.internal.impl.DiscordBotImpl

@DslMarker
annotation class BotConfigDsl

@JvmName("bot")
fun DiscordBot(config: DiscordBot.Config): DiscordBot {
    config.requireToken()
    return DiscordBotImpl(config)
}

@BotConfigDsl
@JvmSynthetic
@JvmName("bot")
inline fun DiscordBot(configure: DiscordBot.Config.() -> Unit) =
    DiscordBot(DiscordBot.Config().apply(configure))

@BotConfigDsl
@JvmSynthetic
inline fun DiscordBot.Config.token(block: () -> String) {
    this.token = block()
}

@BotConfigDsl
@JvmSynthetic
inline fun DiscordBot.Config.shard(block: () -> DiscordBot.ShardInfo) {
    this.shardInfo = block()
}

@BotConfigDsl
@JvmSynthetic
inline fun DiscordBot.Config.sessionHandler(block: () -> SessionHandler) {
    this.sessionHandler = block()
}


@BotConfigDsl
@JvmSynthetic
inline fun DiscordBot.Config.useCompression(block: () -> Boolean) {
    this.useCompression = block()
}
