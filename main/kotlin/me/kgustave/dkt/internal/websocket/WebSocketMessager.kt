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
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.selects.whileSelect
import me.kgustave.dkt.internal.websocket.WebSocketMessager.MessageType.*
import kotlin.coroutines.CoroutineContext

internal class WebSocketMessager(
    scope: CoroutineScope,
    private val webSocket: DiscordWebSocket,
    private val context: CoroutineContext
): CoroutineScope by scope {
    private val guildMembersChunkQueue get() = webSocket.guildMembersChunkQueue
    private val messageQueue           get() = webSocket.messageQueue
    private val lock                   get() = webSocket.lock

    private var lastFailedGuildMembersChunk: Message? = null
    private var lastFailedMessage: Message? = null

    // Because tailrec functions compile down to while loops,
    //and since it all runs on a parallel job to the websocket's
    //main coroutine, volatile is just to be safe.
    // In a future stage of this library, I will probably look into
    //the actual necessity (or lack thereof maybe) of this being
    //volatile, but for now better safe than sorry.
    @Volatile private var shutdown = false

    private var job = null as Job?

    fun start() {
        shutdown = false
        job = launch(context, block = { run() }).also {
            it.invokeOnCompletion { t ->
                if(t is CancellationException) {
                    DiscordWebSocket.Log.debug(
                        "WebSocketMessager has been interrupted. " +
                        "This is most likely due to a shutdown!"
                    )
                }
                job = null
                webSocket.nullifyMessager()
            }
        }
    }

    fun close() {
        shutdown = true
        job?.cancel()
    }

    private tailrec suspend fun run() {
        try {
            // Wait until we're authenticated to send any messages.
            // Note: awaitAuthentication returns false if we shut down.
            if(!awaitAuthentication()) return

            lock.lockInterruptibly()

            // Message priority is:
            // Chunk Member Message > Normal Message
            //
            // Note: the only case this returns null means we shut down while selecting,
            //so we return as we would.
            val message = selectNextMessage() ?: return

            DiscordWebSocket.Log.debug("Sending message to websocket: ${message.type} - ${message.text}")
            if(!webSocket.sendMessage(message.text, queue = true)) {
                if(shutdown) return
                handleRateLimitedMessage(message)
            } else when(message) {
                lastFailedGuildMembersChunk -> lastFailedGuildMembersChunk = null
                lastFailedMessage -> lastFailedMessage = null
            }
        } finally {
            webSocket.unlockIfNeeded()
        }

        run()
    }

    private suspend fun awaitAuthentication(): Boolean {
        if(!webSocket.isAuthenticated) whileSelect {
            onTimeout(500) {
                if(shutdown) false
                else !webSocket.isAuthenticated
            }
        }

        return !shutdown
    }

    private tailrec suspend fun selectNextMessage(retrying: Boolean = false): Message? {
        // Taking a second to talk about why we check if this is shut down so much,
        //the answer is somewhat complex.
        // Simply put, the library is in a state where I want to prevent API abuse
        //or leaking of any kind. Most likely the entire websocket procedure will get
        //some drastic simplifications and optimizations as time goes on, but right
        //now all I'm focused on is how to not cause minor issues.
        if(shutdown) return null

        // If we are retrying to select the next queued message, we skip these
        //failed cases, as this is only checked on the first pass.
        // It's worth noting that the reason for this is because the state of
        //these cases can only change AFTER an attempt to send a message has been made.
        if(!retrying) {
            // the last failed guild member chunk ALWAYS goes next if present
            lastFailedGuildMembersChunk?.let { message -> return message }

            // the last failed message only goes next if the current guild member chunk
            //queue is empty, and if it's even present at this time.
            if(guildMembersChunkQueue.isEmpty)
                lastFailedMessage?.let { message -> return message }
        }

        // The fallback to all of the above cases is a select with a bias towards
        //the guild member chunk queue.
        // If 500 ms elapses before the select is calculated, then this function
        //recurses and we start again from the top. The only difference here is
        //that we skip the prior cases because this entire class is structurally
        //concurrent and (technically) stateless.
        return select {
            // top of food chain
            guildMembersChunkQueue.onReceive { Message(it, GUILD_MEMBER_CHUNK) }

            // bottom of food chain
            messageQueue.onReceive { Message(it, MESSAGE) }

            // timeout, this will recursively apply selectNextMessage now.
            onTimeout(500L) { null }
        } ?: selectNextMessage(retrying = true)
    }

    private suspend fun handleRateLimitedMessage(message: Message) {
        delay(60 * 1000L)
        when(message.type) {
            GUILD_MEMBER_CHUNK -> {
                if(message != lastFailedGuildMembersChunk) {
                    lastFailedGuildMembersChunk = message
                }
            }
            MESSAGE -> {
                if(message != lastFailedMessage) {
                    lastFailedMessage = message
                }
            }
        }
    }

    private data class Message(val text: String, val type: MessageType)

    private enum class MessageType { GUILD_MEMBER_CHUNK, MESSAGE }
}
