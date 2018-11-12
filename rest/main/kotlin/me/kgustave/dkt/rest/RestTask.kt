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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package me.kgustave.dkt.rest

import io.ktor.client.utils.EmptyContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalDktREST
@Deprecated("RestTask is no longer supported!")
abstract class RestTask<T>(protected val requester: DiscordRequester, internal val route: Route) {
    open val headers = headersOf()
    open val rateLimit = true
    open val body: Any get() = EmptyContent

    @Suppress("DEPRECATION")
    open suspend fun await(): T = requester.request(this)

    fun <R> spawn(
        on: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.(T) -> R
    ): Deferred<R> {
        return on.async(context, start) {
            val result = await()
            return@async block(result)
        }
    }

    abstract suspend fun handle(call: DiscordCall): T
}
