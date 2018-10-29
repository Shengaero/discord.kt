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
@file:JvmName("OkHttpEngineUtil")
package me.kgustave.dkt.http.engine

import io.ktor.client.call.UnsupportedContentTypeException
import io.ktor.client.engine.mergeHeaders
import io.ktor.client.request.DefaultHttpRequest
import io.ktor.http.content.OutgoingContent
import io.ktor.util.InternalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.toInputStream
import kotlinx.coroutines.io.writer
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import java.io.IOException
import kotlin.coroutines.*

private class CompletableCallback(call: Call, parent: Job?): Callback {
    private val completion = CompletableDeferred<Response>(parent = parent)

    init {
        completion.invokeOnCompletion {
            if(it is CancellationException) call.cancel()
        }
    }

    suspend fun await() = completion.await()

    override fun onFailure(call: Call, e: IOException) {
        completion.cancel(e)
    }

    override fun onResponse(call: Call, response: Response) {
        completion.complete(response)
    }
}

private class StreamRequestBody(
    private val parent: Job? = null,
    private val block: () -> ByteReadChannel
): RequestBody() {
    override fun contentType(): MediaType? = null
    override fun writeTo(sink: BufferedSink) {
        Okio.source(block().toInputStream(parent)).use { sink.writeAll(it) }
    }
}

internal suspend fun Call.await(): Response {
    val callback = CompletableCallback(this, coroutineContext[Job])
    enqueue(callback)
    return callback.await()
}

internal fun convertToOkHttpBody(content: OutgoingContent): RequestBody? = when(content) {
    is OutgoingContent.ByteArrayContent -> RequestBody.create(null, content.bytes())
    is OutgoingContent.ReadChannelContent -> StreamRequestBody { content.readFrom() }
    is OutgoingContent.ProtocolUpgrade, is OutgoingContent.NoContent -> null
    is OutgoingContent.WriteChannelContent -> StreamRequestBody body@ {
        val job = GlobalScope.writer(Dispatchers.IO) { content.writeTo(channel) }
        return@body job.channel
    }
    else -> throw UnsupportedContentTypeException(content)
}

@UseExperimental(InternalAPI::class)
internal fun DefaultHttpRequest.toOkRequest(): Request = Request.Builder().apply {
    url(url.toString())
    mergeHeaders(headers, content) { key, value -> addHeader(key, value) }
    method(method.value, convertToOkHttpBody(content))
}.build()
