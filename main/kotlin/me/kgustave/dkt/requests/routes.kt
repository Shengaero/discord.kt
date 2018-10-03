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
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.formUrlEncode

sealed class Route {
    abstract val method: HttpMethod
    abstract val path: String
    abstract val parameters: List<String>
    abstract val rateLimit: RateLimit?
    abstract val missingHeaders: Boolean
    abstract val queryParams: List<Pair<String, Any?>>

    abstract fun withQuery(vararg queryParams: Pair<String, Any?>): Route

    override fun toString() = path + queryParams.map { it.first to "${it.second}" }.formUrlEncode()

    data class RateLimit(val limit: Int, val reset: Long)

    companion object {
        val GetGatewayBot = route(Get, "/gateway/bot")

        val GetUser = route(Get, "/users/{user.id}", listOf("user.id"))

        val GetGuild = route(Get, "/guilds/{guild.id}", listOf("guild.id"))

        val CreateDM = route(Post, "/users/@me/channels")
    }
}

// TODO add more params when necessary, once complete, extract from internals
internal fun route(method: HttpMethod, path: String, parameters: List<String> = emptyList()): Route {
    return BasicRoute(method, path, parameters)
}

data class BasicRoute(
    override val method: HttpMethod,
    override val path: String,
    override val parameters: List<String> = emptyList(),
    override val rateLimit: RateLimit? = null,
    override val missingHeaders: Boolean = false,
    override val queryParams: List<Pair<String, Any?>> = emptyList()
): Route() {
    private val formattablePath: String

    init {
        var formattablePath = path

        for(param in parameters) {
            formattablePath = formattablePath.replace("{$param}", "%s")
        }

        this.formattablePath = formattablePath
    }

    fun format(vararg params: Any): FormattedRoute {
        require(params.size == parameters.size) {
            "Invalid number of parameters. Expected: ${parameters.size} Actual: ${params.size}"
        }

        return FormattedRoute(this, formattablePath.format(*params))
    }

    override fun withQuery(vararg queryParams: Pair<String, Any?>): BasicRoute {
        return copy(queryParams = queryParams.toList())
    }

    override fun toString(): String = super.toString()
}

data class FormattedRoute(
    val base: BasicRoute,
    val formattedPath: String,
    override val queryParams: List<Pair<String, Any?>> = emptyList()
): Route() {
    override val path get() = formattedPath
    override val method get() = base.method
    override val rateLimit get() = base.rateLimit
    override val parameters get() = base.parameters
    override val missingHeaders get() = base.missingHeaders

    override fun withQuery(vararg queryParams: Pair<String, Any?>): FormattedRoute {
        return copy(queryParams = queryParams.toList())
    }

    override fun toString(): String = super.toString()
}

object FakeRoute: Route() {
    override val method = HttpMethod("FAKE")
    override val missingHeaders = true
    override val parameters = emptyList<String>()
    override val path = "/"
    override val queryParams = emptyList<Pair<String, Any?>>()
    override val rateLimit: RateLimit? = null

    override fun withQuery(vararg queryParams: Pair<String, Any?>) = this
}
