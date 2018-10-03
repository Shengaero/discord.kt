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
package me.kgustave.dkt.internal.rest

import io.ktor.client.utils.EmptyContent
import io.ktor.http.Headers
import io.ktor.http.headersOf
import me.kgustave.dkt.requests.RestTask
import me.kgustave.dkt.requests.DiscordCall
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route

internal inline fun <reified T> Requester.restTask(
    route: Route,
    body: Any? = null,
    headers: Headers = headersOf(),
    crossinline handle: suspend (call: DiscordCall) -> T
): RestTask<T> = object: RestTask<T>(this, route) {
    override val body: Any get() = body ?: EmptyContent
    override val headers: Headers get() = headers
    override suspend fun handle(call: DiscordCall): T = handle(call)
}
