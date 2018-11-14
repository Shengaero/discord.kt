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
package me.kgustave.dkt.http.engine.websockets

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.response.HttpResponseContainer
import io.ktor.client.response.HttpResponsePipeline
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.isWebsocket
import io.ktor.util.AttributeKey
import me.kgustave.dkt.http.engine.DiscordKtHttpEngineAPI

@Deprecated(
    "Renamed to DiscordWebSocketSession to prevent naming confusion with " +
    "ktor's official interface. This will be removed shortly.",
    ReplaceWith("DiscordWebSocketSession"), DeprecationLevel.ERROR
)
@UseExperimental(DiscordKtHttpEngineAPI::class)
typealias ClientWebSocketSession = DiscordWebSocketSession

@DiscordKtHttpEngineAPI
interface DiscordWebSocketSession: WebSocketSession {
    val call: HttpClientCall

    val isOpen: Boolean
}

@DiscordKtHttpEngineAPI
class WebSockets private constructor() {
    companion object Feature: HttpClientFeature<Unit, WebSockets> {
        override val key = AttributeKey<WebSockets>("Websocket")

        override fun prepare(block: Unit.() -> Unit) = WebSockets()
        override fun install(feature: WebSockets, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) {
                if(!context.url.protocol.isWebsocket()) return@intercept
                proceedWith(WebSocketContent())
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Transform) { (info, response) ->
                val content = context.request.content

                if(!WebSocketSession::class.java.isAssignableFrom(info.type.java)
                   || response !is OkHttpWebSocketResponse
                   || response.status != HttpStatusCode.SwitchingProtocols
                   || content !is WebSocketContent) return@intercept

                content.verify(response.headers)

                val session = response.session

                proceedWith(HttpResponseContainer(info, session))
            }
        }
    }
}
