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

import io.ktor.client.request.ClientUpgradeContent
import io.ktor.client.utils.buildHeaders
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders.Connection
import io.ktor.http.HttpHeaders.SecWebSocketAccept
import io.ktor.http.HttpHeaders.SecWebSocketKey
import io.ktor.http.HttpHeaders.SecWebSocketVersion
import io.ktor.http.HttpHeaders.Upgrade
import io.ktor.util.KtorExperimentalAPI
import java.util.*

internal class WebSocketContent: ClientUpgradeContent() {
    private val nonce = Base64.getEncoder().encodeToString(ByteArray(24).also(rand::nextBytes))

    @KtorExperimentalAPI
    override val headers = buildHeaders {
        this[Upgrade] = "websocket"
        this[Connection] = "upgrade"
        this[SecWebSocketKey] = nonce
        this[SecWebSocketVersion] = "13"
    }

    override fun verify(headers: Headers) {
        val serverAccept = headers[SecWebSocketAccept]
        checkNotNull(serverAccept) { "Server should specify header $SecWebSocketAccept" }

        // FIXME We should (for security) verify the SecWebSocketAccept here eventually
    }

    private companion object {
        private val rand = Random()
    }
}
