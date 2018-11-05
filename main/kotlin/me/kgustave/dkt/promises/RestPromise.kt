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
package me.kgustave.dkt.promises

import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.requests.RestTask
import me.kgustave.dkt.requests.Route
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

abstract class RestPromise<T>
internal constructor(protected val bot: DiscordBotImpl, route: Route): RestTask<T>(bot.requester, route) {
    companion object {
        private var defaultFailureHandle = { t: Throwable ->

        }

        fun defaultFailure(block: (t: Throwable) -> Unit) {
            defaultFailureHandle = block
        }
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
}
