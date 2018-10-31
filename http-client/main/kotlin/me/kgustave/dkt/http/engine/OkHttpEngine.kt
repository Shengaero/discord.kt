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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.io.ByteReadChannel
import me.kgustave.dkt.http.engine.websockets.OkHttpWebSocketResponse
import me.kgustave.dkt.http.engine.websockets.OkHttpWebSocketSession
import okhttp3.OkHttpClient
import okhttp3.Response

@DiscordKtHttpEngineAPI
internal class OkHttpEngine(override val config: OkHttp.Config): HttpClientEngine {
    internal val engine = OkHttpClient.Builder().apply(config.config).build()
    override val dispatcher = engine.dispatcher().executorService().asCoroutineDispatcher()
    override val coroutineContext = SupervisorJob()

    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = DefaultHttpRequest(call, data)
        val requestTime = GMTDate()
        if(request.url.protocol.isWebsocket()) {
            val session = OkHttpWebSocketSession(this, Job(parent = coroutineContext), call, true, Long.MAX_VALUE)
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

    private fun createOkResponse(call: HttpClientCall, requestTime: GMTDate, response: Response): OkHttpResponse {
        // dispatch this on IO to offset read blocking of the created channel
        val responseContent =
            @Suppress("EXPERIMENTAL_API_USAGE")
            response.body()?.byteStream()
                ?.toByteReadChannel(context = Dispatchers.IO, parent = coroutineContext) ?: ByteReadChannel.Empty
        return OkHttpResponse(response, Job(parent = coroutineContext), call, requestTime, responseContent)
    }
}
