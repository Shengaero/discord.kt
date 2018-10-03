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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt.internal.websocket

internal object OP {
    const val Event = 0
    const val Heartbeat = 1
    const val Identify = 2
    const val Resume = 6
    const val Reconnect = 7
    const val InvalidSession = 9
    const val Hello = 10
    const val HeartbeatACK = 11

    fun name(op: Int): String = when(op) {
        Event -> "Event"
        Heartbeat -> "Heartbeat"
        Identify -> "Identify"
        Resume -> "Resume"
        Reconnect -> "Reconnect"
        InvalidSession -> "InvalidSession"
        Hello -> "Hello"
        HeartbeatACK -> "HeartbeatACK"

        else -> "Unknown"
    }

    fun isValid(op: Int): Boolean {
        return op == Event || op == Heartbeat || op == Identify || op == Resume || op == Reconnect ||
               op == InvalidSession || op == Hello || op == HeartbeatACK
    }
}
