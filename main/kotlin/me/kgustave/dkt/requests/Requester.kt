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

package me.kgustave.dkt.requests

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.*
import me.kgustave.dkt.DiscordKt
import me.kgustave.dkt.handle.SessionHandler
import me.kgustave.dkt.internal.handle.DefaultSessionHandler
import me.kgustave.dkt.util.createLogger
import java.io.Closeable
import java.util.concurrent.TimeUnit

class Requester(
    private val client: HttpClient,
    private val token: String,
    val rateLimitDispatcher: CoroutineDispatcher,
    val shutdownDispatcher: Boolean,
    val sessionHandler: SessionHandler = DefaultSessionHandler
) {
    val rateLimiter = RateLimiter(this)

    suspend fun <T> request(task: RestTask<T>): T {
        val request = DiscordRequest(task)
        val response = request(request)
        return task.handle(DiscordCall(request, response))
    }

    // left as it's own method for testing
    suspend fun request(request: DiscordRequest): DiscordResponse {
        if(request.rateLimit) rateLimiter.submit(request) else {
            request.completion.complete(execute(request, true))
        }

        return request.completion.await()
    }

    suspend fun execute(request: DiscordRequest, handleRateLimit: Boolean, retried: Boolean = false): DiscordResponse {
        val route = request.route

        require(route !is FakeRoute) { "Cannot make request to FakeRoute!" }
        require(route !is BasicRoute || route.parameters.isEmpty()) {
            "Cannot make a request to an unformatted route with required parameters!"
        }

        var retryAfter = rateLimiter.rateLimitFor(route)
        if(retryAfter != null) {
            val response = RateLimitedDiscordResponse(null, retryAfter)
            if(handleRateLimit) {
                request.completion.complete(response)
            }
            return response
        }

        val responses = arrayOfNulls<HttpResponse>(4)
        val rays = hashSetOf<String>()
        var run = 0
        var response: HttpResponse? = null

        try {
            do {
                val call = client.call {
                    method = route.method
                    body = request.body
                    url("$BaseUrl/${route.toString().removePrefix("/")}")
                    contentType(JsonContentType)
                    userAgent(UserAgent)
                    header(HttpHeaders.Authorization, "Bot $token")
                    header(HttpHeaders.AcceptEncoding, "gzip")
                    headers.appendAll(request.headers)
                }

                response = call.response
                responses[run] = response
                response.headers["CF-RAY"]?.let { ray -> rays += ray }

                // successful response from discord
                //break and handle later
                if(response.status.value < 500) break

                run++
                log.debug("Request for ${call.request.method} - ${call.request.url.encodedPath} " +
                                                                       "returned a ${response.status.value}! Retrying... (attempt $run)")

                try { delay(50L * run, TimeUnit.MILLISECONDS) } catch(e: CancellationException) {
                    // cancellation, we cannot continue suspending,
                    //we need to start resuming now!
                    break
                }

                // I don't know if this is possible, but it's better than
                //it being possible and finding out with an NPE...
                requireNotNull(response) { "What?" }
            } while(run < responses.size && response!!.status.value >= 500)

            // see above
            requireNotNull(response) { "What?" }

            if(response.status.value >= 500) {
                return BasicDiscordResponse(response, rays)
            }

            retryAfter = rateLimiter.handleResponse(request.route, response)

            if(retryAfter == null) {
                return BasicDiscordResponse(response, rays)
            }

            return RateLimitedDiscordResponse(response, retryAfter, rays)
        } catch(t: Throwable) {
            if(!retried) {
                return execute(request, handleRateLimit, true)
            }
            return ErrorDiscordResponse(response, t, rays)
        } finally {
            responses.forEach { it?.close() }
        }
    }

    fun shutdown() {
        if(shutdownDispatcher && rateLimitDispatcher is Closeable) {
            rateLimitDispatcher.close()
        }
        rateLimiter.shutdown()
    }

    companion object {
        private val log = createLogger(Requester::class)

        const val BaseUrl = "https://discordapp.com/api/v${DiscordKt.RESTVersion}"
        const val CDNBaseUrl = "https://cdn.discordapp.com/"
        const val UserAgent = "DiscordBot (${DiscordKt.GitHub}, ${DiscordKt.Version.Number})"
        val JsonContentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)
    }
}
