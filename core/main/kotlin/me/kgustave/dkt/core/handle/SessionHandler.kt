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
@file:Suppress("DeprecatedCallableAddReplaceWith", "unused")
package me.kgustave.dkt.core.handle

import me.kgustave.dkt.core.internal.websocket.WebSocketConnection
import me.kgustave.dkt.rest.GlobalRateLimitProvider

interface SessionHandler: GlobalRateLimitProvider {
    fun queueConnection(connection: WebSocketConnection)
    fun dequeueConnection(connection: WebSocketConnection)

    fun shutdown() {}

    @Deprecated(
        message = "No replacement. Configure in DiscordBot.Config",
        level = DeprecationLevel.ERROR
    )
    val dispatcherProvider: DispatcherProvider
        get() = throw UnsupportedOperationException("Configure in DiscordBot.Config!")
}
