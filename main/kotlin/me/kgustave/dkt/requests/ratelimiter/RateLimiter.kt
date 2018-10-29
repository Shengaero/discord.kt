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
package me.kgustave.dkt.requests.ratelimiter

import io.ktor.client.response.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import me.kgustave.dkt.handle.SessionHandler
import me.kgustave.dkt.requests.*
import me.kgustave.dkt.util.*
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

class RateLimiter(
    private val requester: Requester,
    private val dispatcher: CoroutineDispatcher,
    private val global: GlobalRateLimitProvider
) {
    private companion object {
        private val Log = createLogger(RateLimiter::class)

        private const val XRateLimitGlobal = "X-RateLimit-Global"
        private const val XRateLimitReset = "X-RateLimit-Reset"
        private const val XRateLimitLimit = "X-RateLimit-Limit"
        private const val XRateLimitRemaining = "X-RateLimit-Remaining"
    }

    @Volatile private var isShutdown = false

    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val running = ConcurrentHashMap<Bucket, Job>()

    // Default -1L
    private var offset = -1L

    private val now: Long get() = currentTimeMs + max(offset, 0L)

    @Deprecated("in order to allow extraction, this will be removed in a future release", level = DeprecationLevel.ERROR)
    private val sessionHandler get() = global as SessionHandler

    fun submit(request: DiscordRequest) {
        reject(isShutdown) { "Cannot queue requests while RateLimiter is closing or shutdown!" }

        bucketFor(request.route).queue(request)
    }

    fun rateLimitFor(route: Route): Long? {
        val bucket = bucketFor(route)
        return synchronized(bucket) { bucket.rateLimitTime() }
    }

    fun handleResponse(route: Route, response: HttpResponse): Long? {
        val bucket = bucketFor(route)
        val now = currentTimeMs
        synchronized(bucket) {
            val headers = response.headers
            val status = response.status

            if(offset == -1L) headers[HttpHeaders.Date]?.let { date ->
                val dateTime = parseOffsetDateTime(date, RFC_1123_DATE_TIME)
                offset = dateTime.toInstant().toEpochMilli() - now
                Log.debug("Set RateLimiter time offset to $offset ms")
            }

            if(status.value == 429) {
                val global = headers[XRateLimitGlobal]
                val text = response.readBody()
                val body = JsonParser.parse<RateLimitedResponse>(text)
                val retryAfter = headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: body.retryAfter
                Log.debug("RateLimit received: $body")

                if(global?.toBoolean() == true) {
                    this.global.globalRateLimit = this.now + retryAfter
                } else {
                    bucket.update(headers, retryAfter)
                }

                return retryAfter
            } else {
                bucket.update(headers)
                return null
            }
        }
    }

    fun shutdown() {
        isShutdown = true
    }

    private fun bucketFor(route: Route): Bucket {
        synchronized(buckets) {
            return buckets.computeIfAbsent(route.rateLimitedPath) { Bucket(route) }
        }
    }

    private inner class Bucket(route: Route) {
        val path = route.rateLimitedPath
        val rateLimit = route.rateLimit
        val missingHeaders = route.missingHeaders
        val requestLock = Mutex() // lock for the queue

        @Volatile var limit = rateLimit?.limit ?: 1
        @Volatile var remaining = limit
        @Volatile var reset = 0L // ms

        private val queue = ConcurrentLinkedQueue<DiscordRequest>()

        fun rateLimitTime(): Long? {
            val globalCooldown = global.globalRateLimit
            if(globalCooldown > 0) {
                val now = now
                if(now < globalCooldown) {
                    return globalCooldown - now
                }
                global.globalRateLimit = Long.MIN_VALUE
            }

            if(remaining <= 0) {
                if(now > reset) {
                    remaining = limit
                    reset = 0
                }
            }

            if(remaining > 0) return null
            return reset - now
        }

        // used to add a request to the queue
        fun queue(request: DiscordRequest) {
            queue += request
            process()
        }

        // used to update the bucket
        fun update(headers: Headers, retryAfter: Long? = null) {
            var count = 0

            if(retryAfter != null) {
                this.reset = now + retryAfter
                this.remaining = 0
            }

            if(rateLimit != null) {
                this.reset = now + rateLimit.reset
            } else {
                headers[XRateLimitReset]?.toLongOrNull()?.let { reset ->
                    this.reset = reset * 1000 // s -> ms
                    count++
                }
                headers[XRateLimitLimit]?.toIntOrNull()?.let { limit ->
                    this.limit = limit
                    count++
                }
            }

            headers[XRateLimitRemaining]?.toIntOrNull()?.let { remaining ->
                this.remaining = remaining
                count++
            }

            if(!missingHeaders && count < 3) {
                val headerString = headers.entries().joinToString("\n") { (header, values) ->
                    values.joinToString("\n") { value ->
                        "$header: $value"
                    }
                }
                Log.debug("Bucket was updated improperly for {}\n{}", path, headerString)
            }
        }

        // process requests
        // if we are already in the process of running requests in
        //the queue, this won't do anything
        private fun process() {
            synchronized(running) {
                if(this !in running) {
                    running[this] = start()
                }
            }
        }

        // this is called when we need to process remaining requests
        private fun start(): Job = GlobalScope.launch(dispatcher) {
            requestLock.lock(queue)
            try {
                val iterator = queue.iterator()
                while(iterator.hasNext()) {
                    Log.debug("Processing request for Bucket: ${this@Bucket}")
                    val request = iterator.next()
                    try {
                        if(request.completion.isCancelled) continue
                        rateLimitTime()?.let {
                            Log.debug("Delaying next request to $path for $it milliseconds.")
                            delay(it)
                        }
                        val response = requester.execute(request, false)
                        if(response is RateLimitedDiscordResponse) break else {
                            iterator.remove()
                            request.completion.complete(response)
                        }
                    } catch(e: Exception) {
                        iterator.remove()
                        request.completion.cancel(e)
                    }
                }

                synchronized(running) {
                    running -= this@Bucket
                    if(queue.isNotEmpty()) {
                        process()
                    }
                }
            } finally {
                if(requestLock.isLocked) requestLock.unlock(queue)
            }
        }

        override fun hashCode(): Int = path.hashCode()

        override fun equals(other: Any?): Boolean {
            if(other !is Bucket) return false
            return path == other.path
        }

        override fun toString(): String {
            return "Bucket('$path' [${queue.size}], usage=$remaining/$limit, reset=${reset}ms)"
        }
    }
}
