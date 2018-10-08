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
package me.kgustave.dkt.internal.websocket

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal class WebSocketMessager(
    private val scope: CoroutineScope,
    private val webSocket: DiscordWebSocket,
    private val context: CoroutineContext
): AutoCloseable {
    //private val bot = webSocket.bot
    private val guildMembersChunkQueue = webSocket.guildMembersChunkQueue
    private val messageQueue = webSocket.messageQueue
    private val lock = webSocket.lock

    private var needRateLimit = false
    private var attemptedToSend = false
    private var shutdown = false
    private var job = null as Job?

    fun start() {
        shutdown = false
        job = scope.launch(context, block = { run() }).also {
            it.invokeOnCompletion { webSocket.nullifyMessager() }
        }
    }

    override fun close() {
        shutdown = true
        job?.cancel()
    }

    private tailrec suspend fun CoroutineScope.run() {
        if(!isActive || shutdown) return

        try {
            // wait until we're authenticated to
            //send any messages
            while(!webSocket.authenticated) delay(500)

            attemptedToSend = false
            needRateLimit = false
            lock.lockInterruptibly()

            if(!sendNextGuildMemberChunkMessage()) {
                sendNextMessage()
            }

            when {
                needRateLimit -> delay(60 * 1000L) // 60 seconds
                !attemptedToSend -> delay(500L)
            }
        } catch(t: Throwable) {
            when(t) {
                is CancellationException, is InterruptedException -> {
                    Log.debug(
                        "WebSocketMessager has been interrupted. " +
                        "This is most likely due to a shutdown!"
                    )
                    return
                }
                else -> throw t
            }
        } finally {
            webSocket.unlockIfNeeded()
        }

        run()
    }

    private suspend fun sendNextGuildMemberChunkMessage(): Boolean {
        val text = guildMembersChunkQueue.peek() ?: return false
        Log.debug("Sending message to websocket: $text")
        if(send(text)) guildMembersChunkQueue.remove()
        return true
    }

    private suspend fun sendNextMessage() {
        val text = messageQueue.peek() ?: return
        Log.debug("Sending message to websocket: $text")
        if(send(text)) messageQueue.remove()
    }

    private suspend fun send(text: String): Boolean {
        needRateLimit = !webSocket.sendMessage(text, queue = true)
        attemptedToSend = true
        return !needRateLimit
    }

    private companion object {
        private val Log = DiscordWebSocket.Log
    }
}

internal fun CoroutineScope.webSocketMessager(
    webSocket: DiscordWebSocket,
    context: CoroutineContext
) = WebSocketMessager(this, webSocket, context)
