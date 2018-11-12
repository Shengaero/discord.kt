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
package me.kgustave.dkt.rest.internal

import io.ktor.client.response.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import me.kgustave.dkt.rest.*
import me.kgustave.dkt.rest.util.readBody
import me.kgustave.dkt.util.*
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

@UseExperimental(ExperimentalDktREST::class)
internal class RateLimiter internal constructor(
    private val requester: DiscordRequester,
    private val dispatcher: CoroutineDispatcher,
    private val global: GlobalRateLimitProvider,

    // Note: this needs more testing before it can
    //considered to be stable. I'm unsure exactly how
    //channels select when to use a thread, and this
    //needs to be documented by kotlinx.coroutines a
    //bit more before I'll consider this as a stable
    //feature.
    // This all being said, if it does work and doesn't
    //have too much impact, I am going to prefer using
    //this option, as it removes a large chunk of the
    //synchronization horror from this system, something
    //I am always actively pursuing.
    private val useExperimentalBuckets: Boolean
) {
    private companion object {
        private val Log = createLogger(RateLimiter::class)

        private const val XRateLimitGlobal = "X-RateLimit-Global"
        private const val XRateLimitReset = "X-RateLimit-Reset"
        private const val XRateLimitLimit = "X-RateLimit-Limit"
        private const val XRateLimitRemaining = "X-RateLimit-Remaining"
    }

    @Volatile private var isShutdown = false

    private val buckets = ConcurrentHashMap<String, IBucket>()
    private val running = ConcurrentHashMap<Bucket, Job>()

    // Default -1L
    private var offset = -1L

    private val now get() = currentTimeMs + max(offset, 0L)

    fun submit(request: DiscordRequest) {
        reject(isShutdown) { "Cannot queue requests while RateLimiter is closing or shutdown!" }

        bucketFor(request.route).queue(request)
    }

    fun rateLimitFor(route: Route): Long? {
        val bucket = bucketFor(route)
        return synchronized(bucket) { bucket.rateLimitTime() }
    }

    @UseExperimental(ImplicitReflectionSerializer::class)
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

                // TODO
                // As of moving the REST request part of the library to it's own
                //module, this is the only usage of the kotlinx-serialization API
                //in this particular module. While I consider this not too important,
                //as many people will probably opt to use kotlinx-serialization
                //alongside this module (even as a standalone), I don't think it's
                //entirely appropriate to package such a bulky library for a single
                //line of code.
                // This is more of a warning to please explicitly include
                //kotlinx-serialization in your project's dependencies, as I'm not
                //entirely sure this will always be included in the future!
                val body = JSON.parse<RateLimitedResponse>(text)
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

    private fun bucketFor(route: Route): IBucket {
        synchronized(buckets) {
            return buckets.computeIfAbsent(route.rateLimitedPath) {
                if(useExperimentalBuckets) BucketEx(route) else Bucket(route)
            }
        }
    }

    private interface IBucket {
        val path: String
        val rateLimit: Route.RateLimit?
        val missingHeaders: Boolean

        var limit: Int
        var remaining: Int
        var reset: Long

        fun rateLimitTime(): Long?

        // used to add a request to the queue
        fun queue(request: DiscordRequest)

        // used to update the bucket
        fun update(headers: Headers, retryAfter: Long? = null)
    }

    // Normal bucket that uses a job
    private inner class Bucket(route: Route): IBucket {
        override val path = route.rateLimitedPath
        override val rateLimit = route.rateLimit
        override val missingHeaders = route.missingHeaders
        val requestLock = Mutex() // lock for the queue

        @Volatile override var limit = rateLimit?.limit ?: 1
        @Volatile override var remaining = limit
        @Volatile override var reset = 0L // ms

        private val queue = ConcurrentLinkedQueue<DiscordRequest>()

        override fun rateLimitTime(): Long? {
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

        override fun queue(request: DiscordRequest) {
            queue += request
            process()
        }

        override fun update(headers: Headers, retryAfter: Long?) {
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
                Log.debug("Bucket was updated improperly for $path\n$headerString")
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
                        request.completion.completeExceptionally(e)
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
            return "Bucket(path=\"$path\", size=${queue.size}, usage=$remaining/$limit, reset=${reset}ms)"
        }
    }

    // Experimental bucket using channels
    @UseExperimental(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private inner class BucketEx(route: Route): IBucket {
        override val path = route.rateLimitedPath
        override val rateLimit = route.rateLimit
        override val missingHeaders = route.missingHeaders

        @Volatile override var limit = rateLimit?.limit ?: 1
        @Volatile override var remaining = limit
        @Volatile override var reset = 0L // ms

        private val queue = GlobalScope.actor<DiscordRequest>(dispatcher, Channel.UNLIMITED) {
            var rateLimited: DiscordRequest? = null
            while(!isClosedForReceive) {
                val request = rateLimited?.also { rateLimited = null } ?: receiveOrNull() ?: break
                Log.debug("Processing request for Bucket: ${this@BucketEx}")
                try {
                    if(request.completion.isCancelled) continue
                    rateLimitTime()?.let {
                        Log.debug("Delaying next request to $path for $it milliseconds.")
                        delay(it)
                    }

                    val response = requester.execute(request, false)
                    if(response is RateLimitedDiscordResponse) {
                        rateLimited = request
                    } else {
                        request.completion.complete(response)
                    }
                } catch(t: Throwable) {
                    request.completion.completeExceptionally(t)
                }
            }
        }

        override fun rateLimitTime(): Long? {
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

        override fun queue(request: DiscordRequest) {
            queue.offer(request)
        }

        override fun update(headers: Headers, retryAfter: Long?) {
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
                    values.joinToString("\n") { value -> "$header: $value" }
                }
                Log.debug("BucketEx was updated improperly for $path\n$headerString")
            }
        }

        override fun hashCode(): Int = path.hashCode()

        override fun equals(other: Any?): Boolean {
            if(other !is Bucket) return false
            return path == other.path
        }

        override fun toString(): String {
            return "BucketEx(path=\"$path\", usage=$remaining/$limit, reset=${reset}ms)"
        }
    }
}
