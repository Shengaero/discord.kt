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
import kotlinx.coroutines.selects.whileSelect
import kotlin.coroutines.CoroutineContext

internal class WebSocketMessager(
    private val scope: CoroutineScope,
    private val webSocket: DiscordWebSocket,
    private val context: CoroutineContext
): AutoCloseable {

    // TODO
    // I believe this could be changed to a set of coroutine channels
    //as opposed to queues.
    // Right now I'm sticking to queues because
    //they contain size info and have functions that allow peeking into
    //the head of the queue, stuff that coroutine channels currently
    //don't have.
    // It MAY be worth investigating into creating our own channel
    //implementation that uses queues in the background as a buffer,
    //although I'll leave that bridge until we cross it.
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

    private tailrec suspend fun run() {
        if(shutdown) return

        try {
            // wait until we're authenticated to
            //send any messages
            awaitAuthentication()

            attemptedToSend = false
            needRateLimit = false
            lock.lockInterruptibly()

            // The messaging priority is:
            // Chunk Sync Message > Normal Message
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

    private suspend fun awaitAuthentication() {
        if(webSocket.isAuthenticated) return
        whileSelect { onTimeout(500) { !webSocket.isAuthenticated } }
    }

    private suspend fun sendNextGuildMemberChunkMessage(): Boolean {
        val text = guildMembersChunkQueue.peek() ?: return false
        if(send(text)) guildMembersChunkQueue.remove()
        return true
    }

    private suspend fun sendNextMessage() {
        val text = messageQueue.peek() ?: return
        if(send(text)) messageQueue.remove()
    }

    private suspend fun send(text: String): Boolean {
        Log.debug("Sending message to websocket: $text")
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
