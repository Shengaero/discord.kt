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
package me.kgustave.dkt.exceptions

import java.lang.RuntimeException

/**
 * Exception thrown when calls to functions or properties are made on an instance
 * of some entity that has yet to completely load.
 *
 * This is used heavily when guilds are unavailable for example:
 *
 * ```kotlin
 * suspend fun main(args: Array<String>) {
 *     val bot = DiscordBot {
 *         // ...
 *         startAutomatically { true }
 *     }
 *
 *     // at this point, "someGuild" is unavailable or not loaded completely.
 *     val someGuild = bot.guildCache[getAGuildId()]!!
 *
 *     // throws UnloadedPropertyException
 *     val name = someGuild.name
 *
 *     println(name)
 * }
 * ```
 */
class UnloadedPropertyException(message: String): RuntimeException(message)
