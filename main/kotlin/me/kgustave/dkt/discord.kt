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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "FunctionName", "DeprecatedCallableAddReplaceWith")
package me.kgustave.dkt

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.handle.*
import me.kgustave.dkt.internal.impl.DiscordBotImpl

@DslMarker
annotation class BotConfigDsl

@JvmName("bot") fun DiscordBot(config: DiscordBot.Config): DiscordBot {
    config.requireToken()
    return DiscordBotImpl(config)
}

@BotConfigDsl
inline fun DiscordBot(configure: DiscordBot.Config.() -> Unit) = DiscordBot(DiscordBot.Config().apply(configure))

@BotConfigDsl
inline fun DiscordBot.Config.startAutomatically(block: () -> Boolean) {
    this.startAutomatically = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.token(block: () -> String) {
    this.token = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.sessionHandler(block: () -> SessionHandler) {
    this.sessionHandler = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.compression(block: () -> Boolean) {
    this.compression = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.activity(block: () -> Activity?) {
    this.activity = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.afk(block: () -> Boolean) {
    this.afk = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.status(block: () -> OnlineStatus) {
    this.status = block()
}

@BotConfigDsl
inline fun DiscordBot.Config.dispatcherProvider(block: () -> DispatcherProvider) {
    this.dispatcherProvider = block()
}

@BotConfigDsl
@ExperimentalEventListeners
inline fun DiscordBot.Config.eventManager(block: () -> EventManager) {
    this.eventManager = block()
}

@BotConfigDsl
@ExperimentalEventListeners
inline fun <reified E: Event> DiscordBot.Config.on(crossinline block: (event: E) -> Unit) {
    this.eventManager.addListener(object: EventListener {
        override fun on(event: Event) {
            if(event is E) block(event)
        }
    })
}

@BotConfigDsl
@Suppress("DEPRECATION")
@Deprecated("sharding will be moved to a separate interface with an expanded configuration DSL")
inline fun DiscordBot.Config.shard(block: () -> DiscordBot.ShardInfo) {
    this.shardInfo = block()
}

@BotConfigDsl
@Deprecated("renamed to compression",
    ReplaceWith("compression(block)", imports = ["me.kgustave.dkt.compression"]),
    DeprecationLevel.HIDDEN)
inline fun DiscordBot.Config.useCompression(block: () -> Boolean) = compression(block)
