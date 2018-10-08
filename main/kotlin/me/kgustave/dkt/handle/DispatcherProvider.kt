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
package me.kgustave.dkt.handle

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory

interface DispatcherProvider {
    fun provideRateLimitDispatcher(shardId: Int?): DispatcherInfo {
        val factory = DiscordBotIdentifiableThreadFactory("RateLimiter", shardId)
        val threadpool = ScheduledThreadPoolExecutor(5, factory)
        return DispatcherInfo(threadpool.asCoroutineDispatcher())
    }

    fun providePromiseDispatcher(shardId: Int?): DispatcherInfo {
        return DispatcherInfo(Dispatchers.Default, false)
    }

    data class DispatcherInfo(val dispatcher: CoroutineDispatcher, val shutdownAutomatically: Boolean = true)

    private class DiscordBotIdentifiableThreadFactory(
        private val identity: String,
        private val shardId: Int?
    ): ThreadFactory {
        private val threadId = atomic(0)

        override fun newThread(r: Runnable): Thread {
            val name = when(shardId) {
                null -> "DiscordBot $identity"
                else -> "DiscordBot $identity (Shard ID: $shardId)"
            } + " - Thread ${threadId.getAndIncrement()}"

            val thread = Thread(r, name)
            thread.isDaemon = true
            return thread
        }
    }

    companion object Default: DispatcherProvider
}
