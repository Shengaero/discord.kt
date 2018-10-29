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

import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.encodeURLParameter

private val ParamRegex = Regex("\\{(.*?)}")

/**
 * Creates a [BasicRoute] with the given [method] and [path].
 *
 * Major route [parameters] may be provided as well, and a hardcoded [rateLimit] may
 * be specified if the discord API does not handle it correctly.
 */
fun route(
    method: HttpMethod,
    path: String,
    parameters: List<String> = emptyList(),
    rateLimit: Route.RateLimit? = null,
    missingHeaders: Boolean = false
): BasicRoute = BasicRoute(method, path, parameters, rateLimit, missingHeaders)

sealed class Route {
    abstract val method: HttpMethod
    abstract val path: String
    abstract val rateLimitedPath: String
    abstract val parameters: List<String>
    abstract val rateLimit: RateLimit?
    abstract val missingHeaders: Boolean
    abstract val queryParams: List<Pair<String, String?>>

    abstract fun withQuery(vararg queryParams: Pair<String, Any?>): Route

    final override fun hashCode(): Int = (method.value + path).hashCode()
    final override fun equals(other: Any?): Boolean = other is Route && method == other.method && path == other.path
    final override fun toString(): String {
        if(queryParams.isEmpty()) return path
        return buildString {
            append(path)
            queryParams.joinTo(this, separator = "&", prefix = "?") { (key, value) ->
                return@joinTo "${key.encodeURLParameter()}=${value?.encodeURLParameter()}"
            }
        }
    }

    data class RateLimit(val limit: Int, val reset: Long)

    @Suppress("unused")
    companion object {
        val GetGatewayBot = route(Get, "/gateway/bot")

        val GetUser    = route(Get,    "/users/{user.id}", listOf("user.id"))
        val GetSelf    = route(Get,    "/users/@me", missingHeaders = true)
        val ModifySelf = route(Patch,  "/users/@me")
        val GetDMs     = route(Get,    "/users/@me/channels")
        val CreateDM   = route(Post,   "/users/@me/channels")
        val GetGuilds  = route(Get,    "/users/@me/guilds")
        val LeaveGuild = route(Delete, "/users/@me/guilds/{guild.id}", listOf("guild.id"))

        val CreateGuild = route(Post, "/guilds")
        val GetGuild    = route(Get,  "/guilds/{guild.id}", listOf("guild.id"))

        val GetChannel    = route(Get,    "/channels/{channel.id}", listOf("channel.id"))
        val EditChannel   = route(Patch,  "/channels/{channel.id}", listOf("channel.id"), missingHeaders = true)
        val DeleteChannel = route(Delete, "/channels/{channel.id}", listOf("channel.id"), missingHeaders = true)

        val CreateChannelOverride = route(Get,    "/channels/{channel.id}/permissions/{overwrite.id}", listOf("channel.id"))
        val EditChannelOverride   = route(Patch,  "/channels/{channel.id}/permissions/{overwrite.id}", listOf("channel.id"))
        val DeleteChannelOverride = route(Delete, "/channels/{channel.id}/permissions/{overwrite.id}", listOf("channel.id"))

        val GetMessage    = route(Get,   "/channels/{channel.id}/messages/{message.id}", listOf("channel.id"), missingHeaders = true)
        val GetMessages   = route(Get,   "/channels/{channel.id}/messages",              listOf("channel.id"), missingHeaders = true)
        val CreateMessage = route(Post,  "/channels/{channel.id}/messages",              listOf("channel.id"))
        val EditMessage   = route(Patch, "/channels/{channel.id}/messages/{message.id}", listOf("channel.id"))
        val DeleteMessage = DeleteMessageRoute() as BasicRoute

        val BulkDeleteMessage = route(Delete, "/channels/{channel.id}/messages/bulk-delete", listOf("channel.id"))

        val GetPinnedMessage    = route(Get,    "/channels/{channel.id}/pins",              listOf("channel.id"))
        val AddPinnedMessage    = route(Put,    "/channels/{channel.id}/pins/{message.id}", listOf("channel.id"))
        val DeletePinnedMessage = route(Delete, "/channels/{channel.id}/pins/{message.id}", listOf("channel.id"))
    }
}

open class BasicRoute(
    final override val method: HttpMethod,
    final override val path: String,
    final override val parameters: List<String> = emptyList(),
    final override val rateLimit: RateLimit? = null,
    final override val missingHeaders: Boolean = false,
    final override val queryParams: List<Pair<String, String?>> = emptyList()
): Route() {
    protected val formattablePath = path.replace(ParamRegex, "%s")
    protected val paramCount = path.count { it == '{' }
    protected val majorParameterIndices = arrayListOf<Int>()

    override val rateLimitedPath = run {
        var i = 0
        return@run path.replace(ParamRegex) { result ->
            i++
            val parameter = result.groupValues[1]
            if(parameter !in parameters) {
                return@replace "{$parameter}"
            } else {
                majorParameterIndices += (i - 1)
                return@replace "%s"
            }
        }
    }

    fun format(vararg params: Any): FormattedRoute {
        require(params.size == paramCount) {
            "Invalid number of parameters. Expected: ${parameters.size} Actual: ${params.size}"
        }

        val strings = params.map(Any::toString)
        val path = formattablePath.format(*strings.toTypedArray())
        val rateLimitPath = rateLimitedPath.format(*strings.slice(majorParameterIndices).toTypedArray())
        return FormattedRoute(this, path, rateLimitPath, queryParams)
    }

    override fun withQuery(vararg queryParams: Pair<String, Any?>): BasicRoute {
        return BasicRoute(method, path, parameters, rateLimit, missingHeaders,
            queryParams.map { it.first to it.second?.toString() })
    }
}

class FormattedRoute(
    val base: BasicRoute,
    override val path: String,
    override val rateLimitedPath: String,
    override val queryParams: List<Pair<String, String?>> = emptyList()
): Route() {
    override val method get() = base.method
    override val rateLimit get() = base.rateLimit
    override val parameters get() = base.parameters
    override val missingHeaders get() = base.missingHeaders

    override fun withQuery(vararg queryParams: Pair<String, Any?>): FormattedRoute {
        return FormattedRoute(base, path, rateLimitedPath,
            queryParams.map { it.first to it.second?.toString() })
    }
}

class DeleteMessageRoute(queryParams: List<Pair<String, String?>> = emptyList()): BasicRoute(
    method = Delete,
    path = "/channels/{channel.id}/messages/{message.id}",
    parameters = listOf("channel.id"),
    missingHeaders = true,
    queryParams = queryParams
) {
    override val rateLimitedPath get() = "/channels/%s/messages/{message.id}/delete"
    override fun withQuery(vararg queryParams: Pair<String, Any?>): BasicRoute {
        return DeleteMessageRoute(queryParams.map { it.first to it.second?.toString() })
    }
}

object FakeRoute: Route() {
    override val method = HttpMethod("FAKE")
    override val missingHeaders = true
    override val parameters = emptyList<String>()
    override val path = "/"
    override val rateLimitedPath get() = path
    override val queryParams get() = emptyList<Pair<String, String?>>()
    override val rateLimit: RateLimit? = null

    override fun withQuery(vararg queryParams: Pair<String, Any?>) = this
}
