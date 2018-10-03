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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt

object DiscordKt {
    const val GitHub = "https://github.com/Shengaero/discord.kt"
    const val RESTVersion = 6
    const val GatewayVersion = 6
    object Version {
        const val Major = 0
        const val Minor = 1
        const val Patch = 0
        const val Number = "$Major.$Minor.$Patch"
        override fun toString(): String = Number
    }
}
