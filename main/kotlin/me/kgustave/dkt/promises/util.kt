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
@file:JvmName("RestPromiseUtil")
package me.kgustave.dkt.promises

import io.ktor.http.Headers
import io.ktor.http.headersOf
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.requests.DiscordCall
import me.kgustave.dkt.requests.FakeRoute
import me.kgustave.dkt.requests.Route
import java.lang.UnsupportedOperationException

internal inline fun <reified T> DiscordBotImpl.restPromise(
    route: Route,
    body: Any? = null,
    headers: Headers = headersOf(),
    crossinline handle: suspend (call: DiscordCall) -> T
): RestPromise<T> = object: RestPromise<T>(this, route) {
    override val headers: Headers get() = headers
    override val body: Any get() = body ?: super.body
    override suspend fun handle(call: DiscordCall): T = handle(call)
}

internal fun <T> DiscordBotImpl.emptyPromise(value: T): RestPromise<T> =
    PreCompletedRestTask(this, value)

private class PreCompletedRestTask<T>(bot: DiscordBotImpl, val value: T): RestPromise<T>(bot, FakeRoute) {
    override suspend fun await(): T = value

    override suspend fun handle(call: DiscordCall): T {
        throw UnsupportedOperationException("Handling call is not supported on PreCompletedRestTask!")
    }
}
