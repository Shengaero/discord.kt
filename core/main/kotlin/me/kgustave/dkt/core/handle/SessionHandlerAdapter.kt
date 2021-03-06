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
package me.kgustave.dkt.core.handle

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.kgustave.dkt.core.internal.websocket.WebSocketConnection
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.currentTimeMs
import java.lang.IllegalStateException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException

open class SessionHandlerAdapter: SessionHandler {
    private companion object {
        private const val ConnectionDelay = 5 * 1000L
        private val Log = createLogger(SessionHandler::class)
    }

    protected val global = atomic(Long.MIN_VALUE)
    override var globalRateLimit: Long
        get() = global.value
        set(value) { global.value = value }

    protected val connectionQueue = ConcurrentLinkedQueue<WebSocketConnection>()
    protected val lock = Any()

    private var job: Job? = null
    private var lastConnectTime = 0L
    private val dispatcher = newSingleThreadContext("SessionHandler Queue Dispatcher")

    override fun queueConnection(connection: WebSocketConnection) {
        connectionQueue += connection
        startQueueJob()
    }

    override fun dequeueConnection(connection: WebSocketConnection) {
        connectionQueue.remove(connection)
    }

    private fun startQueueJob() {
        synchronized(lock) {
            if(job != null) return
            job = createJob()
            job!!.start()
        }
    }

    private fun createJob(): Job = GlobalScope.launch(dispatcher, start = CoroutineStart.LAZY) {
        val delay = currentTimeMs - lastConnectTime
        if(delay < ConnectionDelay) delay(delay)

        var multiple = connectionQueue.size > 1
        while(connectionQueue.isNotEmpty()) {
            val connection = connectionQueue.poll() ?: break
            try {
                connection.run(multiple && connectionQueue.isEmpty())
                multiple = true
                lastConnectTime = currentTimeMs
                if(connectionQueue.isEmpty()) break
                delay(ConnectionDelay)
            } catch(e: IllegalStateException) {
                Log.error("Failed to run connection!", e)
                queueConnection(connection)
            } catch(e: CancellationException) {
                // TODO Logging
                queueConnection(connection)
                break
            } catch(e: RejectedExecutionException) {
                break
            }
        }

        synchronized(lock) {
            job = null
            if(connectionQueue.isNotEmpty()) {
                startQueueJob()
            }
        }
    }

    override fun shutdown() {
        job?.cancel()
        dispatcher.close()
    }
}
