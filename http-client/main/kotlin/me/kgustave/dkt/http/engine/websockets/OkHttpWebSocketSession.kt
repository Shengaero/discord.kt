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
package me.kgustave.dkt.http.engine.websockets

import io.ktor.client.call.HttpClientCall
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readReason
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import me.kgustave.dkt.http.engine.DiscordKtHttpEngineAPI
import me.kgustave.dkt.http.engine.OkHttpEngine
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

@DiscordKtHttpEngineAPI
internal class OkHttpWebSocketSession(
    engine: OkHttpEngine,
    private val socketJob: Job,
    override val call: HttpClientCall,
    override var masking: Boolean,
    override var maxFrameSize: Long
): WebSocketListener(), DiscordWebSocketSession {
    private val socketScope = CoroutineScope(socketJob)
    private val readerThread = newSingleThreadContext("OkHttpWebSocketSession Reader")

    private lateinit var webSocket: WebSocket

    @Volatile private var flush: CompletableDeferred<Unit>? = null
    @Volatile private var consumed = false

    // Used to suspend execution until onOpen is called.
    internal val responseDef = CompletableDeferred<Response>()

    // dispatcher + socketJob to regulate child jobs created by this
    override val coroutineContext = engine.dispatcher + socketJob

    // Incoming is a singleton channel buffer so we can take a fast path.
    // Considering that failed offerings default to sending on the reader
    //thread, this should buffer sequentially and fairly.
    override val incoming = Channel<Frame>(1)

    override val isOpen: Boolean get() = !consumed

    // Outgoing is a channel buffer with a maximum capacity of 8 that
    //assures the websocket receives frames sequentially and fairly, but
    //allows the caller to quickly return when sending to this actor.
    override val outgoing = socketScope.actor<Frame>(coroutineContext, capacity = 8, start = LAZY) {
        responseDef.await()
        try {
            loop@ while(!isClosedForReceive) {
                when(val frame = receiveOrNull()) {
                    null -> break@loop // if null, receiver has closed, break

                    is Frame.Text -> webSocket.send(frame.readText())

                    is Frame.Binary,
                    is Frame.Ping,
                    is Frame.Pong -> webSocket.send(ByteString.of(frame.buffer))

                    is Frame.Close -> {
                        val reason = frame.readReason()
                        webSocket.close(reason?.code?.toInt() ?: 1000, reason?.message ?: "")
                        break@loop
                    }
                }

                // If we are empty, complete our current flush
                if(isEmpty) flush?.complete(Unit)
            }
        } catch(t: Throwable) {
            // rethrow the exception to debug
            throw t
        } finally {
            // Always mark this session as consumed
            // and always succeed in completing a flush
            consumed = true
            flush?.complete(Unit)

            // finally cancel
            cancel()
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        this.responseDef.complete(response)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        this.webSocket = webSocket
        if(response == null) responseDef.completeExceptionally(t) else responseDef.complete(response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        checkWebSocketInstance(webSocket)
        queueFrame(Frame.Text(text))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        checkWebSocketInstance(webSocket)
        queueFrame(Frame.Binary(true, bytes.asByteBuffer()))
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        checkWebSocketInstance(webSocket)
        queueFrame(Frame.Close(CloseReason(code.toShort(), reason)))
        consumed = true
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        checkWebSocketInstance(webSocket)
        terminate()
    }

    @KtorExperimentalAPI
    override suspend fun close(cause: Throwable?) {
        outgoing.send(Frame.Close(defaultClose))
        flush()
    }

    override suspend fun flush() {
        // assign a completion to flush
        flush = CompletableDeferred(parent = socketJob)
        // wait for it to complete
        flush!!.await()
        // nullify
        flush = null
    }

    override fun terminate() {
        // we have already consumed this session!
        if(consumed) {
            // make sure to close reader thread!
            return readerThread.close()
        }

        // immediately send a default close
        webSocket.close(defaultClose.code.toInt(), defaultClose.message)

        //val cancel = CancellationException("Connection terminated normally!")

        // cancel the socket job and the incoming channel
        socketJob.cancel()
        incoming.cancel() // FIXME maybe find a way to delegate this close to socketJob

        // close reader thread
        readerThread.close()
    }

    private fun queueFrame(frame: Frame) {
        if(!incoming.offer(frame)) socketScope.launch(readerThread) {
            incoming.send(frame)
        }
    }

    private fun checkWebSocketInstance(webSocket: WebSocket) {
        check(this.webSocket == webSocket) { "WebSocket provided is not valid (Reused listener?!?)" }
    }

    private companion object {
        val defaultClose = CloseReason(1000, "")
    }
}
