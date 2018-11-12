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
package me.kgustave.dkt.rest

import io.ktor.client.call.HttpClientCall
import io.ktor.client.response.HttpResponse
import io.ktor.client.utils.EmptyContent
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.io.ByteReadChannel
import kotlin.coroutines.CoroutineContext

// Calls

@ExperimentalDktREST
data class DiscordCall(val request: DiscordRequest, val response: DiscordResponse)

// Requests

/**
 * Data to provide to a [DiscordRequester] in order to perform a request
 * to the Discord API.
 */
@ExperimentalDktREST
class DiscordRequest(
    val route: Route,
    val headers: Headers = headersOf(),
    val body: Any = EmptyContent,
    val rateLimit: Boolean = true,
    internal val completion: CompletableDeferred<DiscordResponse> = CompletableDeferred()
) {

    @Suppress("DEPRECATION")
    @Deprecated("RestTask is no longer supported!")
    constructor(task: RestTask<*>): this(
        route = task.route,
        headers = task.headers,
        body = task.body,
        rateLimit = task.rateLimit
    )
}

// Responses

@ExperimentalDktREST
sealed class DiscordResponse: HttpResponse {
    abstract val base: HttpResponse?
    abstract val cloudflareRays: Set<String>
}

@ExperimentalDktREST
data class BasicDiscordResponse internal constructor(
    override val base: HttpResponse,
    override val cloudflareRays: Set<String> = emptySet()
): HttpResponse by base, DiscordResponse()

@ExperimentalDktREST
sealed class FailedDiscordResponse: DiscordResponse() {
    abstract val error: Throwable?
    override val call: HttpClientCall get() = base?.call ?: cannotGet("call", error)
    override val status: HttpStatusCode get() = base?.status ?: cannotGet("status", error)
    override val headers: Headers get() = base?.headers ?: cannotGet("headers", error)
    override val version: HttpProtocolVersion get() = base?.version ?: cannotGet("version", error)
    override val content: ByteReadChannel
        get() = base?.content ?: cannotGet("content", error)
    override val coroutineContext: CoroutineContext
        get() = base?.coroutineContext ?: cannotGet("coroutineContext", error)
    override val requestTime: GMTDate
        get() = base?.requestTime ?: cannotGet("requestTime", error)
    override val responseTime: GMTDate
        get() = base?.responseTime ?: cannotGet("responseTime", error)

    override fun close() { base?.close() }
}

@ExperimentalDktREST
data class ErrorDiscordResponse internal constructor(
    override val base: HttpResponse?,
    override val error: Throwable,
    override val cloudflareRays: Set<String> = emptySet()
): FailedDiscordResponse()

@ExperimentalDktREST
data class RateLimitedDiscordResponse internal constructor(
    override val base: HttpResponse?,
    val retryAfter: Long,
    override val cloudflareRays: Set<String> = emptySet()
): FailedDiscordResponse() {
    override val error: Throwable? get() = null
}

private fun cannotGet(name: String, cause: Throwable?): Nothing =
    throw IllegalStateException("Cannot get $name from response!", cause)
