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
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package me.kgustave.dkt.promises

import io.ktor.client.call.receive
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders.ContentDisposition
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
import java.io.File
import java.io.InputStream

class MessagePromise internal constructor(
    bot: DiscordBotImpl,
    private val channel: MessageChannel
): Appendable, RestPromise<Message>(bot, Route.CreateMessage.format(channel.id)) {
    private val files = mutableMapOf<String, Input>()
    private val content = StringBuilder()

    var tts = false
    var nonce = null as String?

    internal constructor(bot: DiscordBotImpl, channel: MessageChannel, content: String): this(bot, channel) {
        this.content.append(content)
    }

    private val json: JsonObject get() = json {
        "content" to content.toString()
        if(tts) { "tts" to true }
        nonce?.let { nonce -> "nonce" to nonce }
    }

    override val body: Any get() {
        if(files.isEmpty()) return json

        // Note, the ktor form dsl is almost the same thing as here, but
        //unfortunately it does not close any of the input data automatically,
        //even though the actual ktor form part API allows for automated disposal
        //of closeable resources???
        // Additionally, the form dsl doesn't even use PartData.FileItem, which
        //is kinda dumb given it supports appending data with a file name???
        // If there are changes, I will consider reimplementing it using the dsl.
        val parts = arrayListOf<PartData>()
        for((i, file) in files.entries.withIndex()) {
            val (filename, data) = file
            parts += PartData.FileItem({ data }, { data.close() }, Headers.build {
                append(ContentDisposition, "form-data;name=file$i")
                append(ContentDisposition, "filename=$filename")
            })
        }

        // append the json content
        parts += PartData.FormItem(json.stringify(), {},
            headersOf(ContentDisposition, "form-data;name=payload_json"))

        files.clear()

        return MultiPartFormDataContent(parts)
    }

    fun appendFile(data: Input, filename: String): MessagePromise {
        require(files.size < Message.MaxFileNumber) {
            "A message may only attach a maximum of ${Message.MaxFileNumber} files!"
        }
        files[filename] = data
        return this
    }

    fun appendFile(data: InputStream, filename: String): MessagePromise = appendFile(data.asInput(), filename)

    fun appendFile(file: File, filename: String = file.name): MessagePromise {
        require(file.length() <= Message.MaxFileSize) {
            "A file attachment may only be up to ${Message.MaxFileSize} bytes!"
        }
        return appendFile(file.inputStream(), filename)
    }

    override fun append(c: Char): MessagePromise {
        checkLength(content.length, 1)
        content.append(c)
        return this
    }

    override fun append(csq: CharSequence): MessagePromise = append(csq, 0, csq.length)
    override fun append(csq: CharSequence, start: Int, end: Int): MessagePromise {
        checkLength(content.length, end - start)
        content.append(csq, start, end)
        return this
    }

    override suspend fun handle(call: DiscordCall): Message {
        val json = call.response.receive<JsonObject>()
        return bot.entities.handleReceivedMessage(json)
    }

    @Deprecated("replace with appendFile", ReplaceWith("appendFile(data, name)"))
    fun addFile(name: String, data: Input): MessagePromise = appendFile(data, name)

    @Deprecated("replace with appendFile", ReplaceWith("appendFile(data, name)"))
    fun addFile(name: String, data: InputStream): MessagePromise = appendFile(data.asInput(), name)

    @Deprecated("replace with appendFile", ReplaceWith("appendFile(file, name)"))
    fun addFile(name: String, file: File): MessagePromise = appendFile(file.inputStream(), name)

    @Deprecated("replace with appendFile", ReplaceWith("appendFile(file)"))
    fun addFile(file: File): MessagePromise = appendFile(file, file.name)

    private companion object {
        @JvmStatic private fun checkLength(current: Int, adding: Int) {
            require(current + adding <= Message.MaxTextLength) {
                "A message may only be up to ${Message.MaxTextLength} characters long!"
            }
        }
    }
}
