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
package me.kgustave.dkt.http.engine

import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.HttpEngineCall
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.DefaultHttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.isWebsocket
import io.ktor.util.cio.toByteReadChannel
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.withContext
import me.kgustave.dkt.http.engine.websockets.OkHttpWebSocketResponse
import me.kgustave.dkt.http.engine.websockets.OkHttpWebSocketSession
import okhttp3.OkHttpClient
import okhttp3.Response

internal class OkHttpEngine(override val config: OkHttp.Config): HttpClientEngine {
    internal val engine = OkHttpClient.Builder().apply(config.config).build()
    override val dispatcher = engine.dispatcher().executorService().asCoroutineDispatcher()
    override val coroutineContext = Job(parent = dispatcher[Job])

    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = DefaultHttpRequest(call, data)
        val requestTime = GMTDate()
        if(request.url.protocol.isWebsocket()) {
            val session = OkHttpWebSocketSession(this, call, true, Long.MAX_VALUE)
            engine.newWebSocket(request.toOkRequest(), session)
            val response = session.responseDef.await()
            val okResponse = createOkResponse(call, requestTime, response)
            val okSocketResponse = OkHttpWebSocketResponse(call, okResponse, session)
            return HttpEngineCall(request, okSocketResponse)
        }
        val response = engine.newCall(request.toOkRequest()).await()
        return HttpEngineCall(request, createOkResponse(call, requestTime, response))
    }

    override fun close() { /* No-op */ }

    private suspend fun createOkResponse(call: HttpClientCall, requestTime: GMTDate, response: Response): OkHttpResponse {
        val responseContent = withContext(dispatcher) {
            response.body()?.byteStream()
                ?.toByteReadChannel(context = coroutineContext) ?: ByteReadChannel.Empty
        }

        return OkHttpResponse(response, coroutineContext, call, requestTime, responseContent)
    }
}
