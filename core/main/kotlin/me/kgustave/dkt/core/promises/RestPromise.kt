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
@file:Suppress("MemberVisibilityCanBePrivate", "MoveLambdaOutsideParentheses")
package me.kgustave.dkt.core.promises

import io.ktor.client.utils.EmptyContent
import io.ktor.http.headersOf
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.rest.DiscordCall
import me.kgustave.dkt.rest.DiscordRequest
import me.kgustave.dkt.rest.Route
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

abstract class RestPromise<T> internal constructor(protected val bot: DiscordBotImpl, internal val route: Route) {
    companion object {
        private var defaultFailureHandle = { t: Throwable ->

        }

        fun defaultFailure(block: (t: Throwable) -> Unit) {
            defaultFailureHandle = block
        }
    }

    open val headers = headersOf()
    open val rateLimit = true
    open val body: Any get() = EmptyContent

    open suspend fun await(): T {
        val request = DiscordRequest(route, headers, body, rateLimit)
        val response = bot.requester.request(request)
        return handle(DiscordCall(request, response))
    }

    fun promise() = promise({})
    fun promise(then: (T) -> Unit) = promise(then, defaultFailureHandle)
    fun promise(then: (T) -> Unit, catch: (Throwable) -> Unit) {
        val cont = Continuation<T>(bot.promiseDispatcher) { result ->
            result.onSuccess(then)
            result.onFailure(catch)
        }

        suspend { this.await() }.startCoroutine(cont)
    }

    abstract suspend fun handle(call: DiscordCall): T
}
