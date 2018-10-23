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
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate", "unused")

package me.kgustave.dkt.promises

import io.ktor.client.call.receive
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.headersOf
import kotlinx.io.core.Input
import kotlinx.io.streams.asInput
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json
import me.kgustave.dkt.entities.Message
import me.kgustave.dkt.entities.MessageChannel
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.requests.DiscordCall
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.util.stringify
import java.io.InputStream
import java.lang.StringBuilder

class MessagePromise internal constructor(
    bot: DiscordBotImpl,
    private val channel: MessageChannel
): RestPromise<Message>(bot, Route.CreateMessage.format(channel.id)), Appendable {
    private val parts = arrayListOf<PartData>()
    private val content = StringBuilder()
    private var fileNumber = 0

    var tts = false

    internal constructor(bot: DiscordBotImpl, channel: MessageChannel, content: String): this(bot, channel) {
        this.content.append(content)
    }

    private val json: JsonObject get() = json {
        "content" to content.toString()
        if(tts) {
            "tts" to true
        }
    }

    override val body: Any get() {
        val body: Any = if(parts.isEmpty()) json else {
            MultiPartFormDataContent(parts + PartData.FormItem(
                value = json.stringify(),
                dispose = {},
                partHeaders = contentDisposition("payload_json")
            ))
        }

        parts.clear()
        fileNumber = 0

        return body
    }

    fun addFile(name: String?, data: Input): MessagePromise {
        parts += PartData.FileItem(
            provider = { data },
            dispose = { data.close() },
            partHeaders = contentDisposition("file${fileNumber++}", name)
        )
        return this
    }

    fun addFile(name: String?, data: InputStream): MessagePromise = addFile(name, data.asInput())

    override fun append(c: Char): MessagePromise {
        content.append(c)
        return this
    }

    override fun append(csq: CharSequence?): MessagePromise {
        content.append(csq)
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): MessagePromise {
        content.append(csq, start, end)
        return this
    }

    override suspend fun handle(call: DiscordCall): Message {
        val json = call.response.receive<JsonObject>()
        return bot.entities.handleReceivedMessage(json)
    }

    private companion object {
        private fun contentDisposition(name: String, filename: String? = null): Headers = headersOf(
            HttpHeaders.ContentDisposition, buildString {
                append("""form-data; name="$name"""")
                filename?.let { append("""; filename="$filename"""") }
            }
        )
    }
}
