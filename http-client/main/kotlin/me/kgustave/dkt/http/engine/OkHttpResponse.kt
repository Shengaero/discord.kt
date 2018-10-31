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
import io.ktor.client.response.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.io.ByteReadChannel
import okhttp3.Protocol
import okhttp3.Response
import kotlin.coroutines.CoroutineContext

@DiscordKtHttpEngineAPI
internal class OkHttpResponse(
    private val response: Response,
    override val coroutineContext: CoroutineContext,
    override val call: HttpClientCall,
    override val requestTime: GMTDate,
    override val content: ByteReadChannel
): HttpResponse {
    override val responseTime = GMTDate()
    override val status = HttpStatusCode.fromValue(response.code())
    override val headers = object: Headers {
        private val instance = response.headers()

        override val caseInsensitiveName: Boolean = false
        override fun getAll(name: String): List<String>? = instance.values(name)
        override fun names(): Set<String> = instance.names()
        override fun entries(): Set<Map.Entry<String, List<String>>> = instance.toMultimap().entries
        override fun isEmpty(): Boolean = instance.size() == 0
    }
    override val version = when(val protocol = response.protocol()) {
        null, Protocol.HTTP_2 -> HttpProtocolVersion.HTTP_2_0 // default HTTP 2/0
        Protocol.HTTP_1_0 -> HttpProtocolVersion.HTTP_1_0
        Protocol.HTTP_1_1 -> HttpProtocolVersion.HTTP_1_1

        else -> throw UnsupportedOperationException("Protocol not supported: $protocol")
    }

    override fun close() {
        runCatching { response.close() }
    }
}
