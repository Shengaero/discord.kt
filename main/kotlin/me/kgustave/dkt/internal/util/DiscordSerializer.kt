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
@file:Suppress("ObsoleteExperimentalCoroutines")
package me.kgustave.dkt.internal.util

import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.client.utils.EmptyContent
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.parse
import kotlinx.serialization.serializer
import me.kgustave.dkt.exceptions.RequestException
import me.kgustave.dkt.rest.DiscordRequester
import me.kgustave.dkt.rest.util.isGzip
import me.kgustave.dkt.rest.util.readBody
import me.kgustave.dkt.util.stringify

/**
 * Serializer for Discord.kt requests
 *
 * Most default ktor deserialization stuff isn't well optimized
 * or doesn't take into consideration some of the complications
 * that come with API specific behavior.
 *
 * @author Kaidan Gustave
 */
internal class DiscordSerializer: JsonSerializer {
    override suspend fun read(type: TypeInfo, response: HttpResponse): Any {
        // Gzip is slightly more complicated to parse using ktor client at the moment
        val text = if(response.isGzip()) response.readBody(true) else {
            // If this isn't gzip we should go for the more optimized response read.
            response.readText(response.charset() ?: Charsets.UTF_8)
        }

        val kotlinType = type.type
        val javaType = kotlinType.java

        val statusCode = response.status.value

        // This is a failed request with a response error. Note: 429 should be handled separately!
        if(RequestException::class.java.isAssignableFrom(javaType) ||
           (statusCode >= 400 && statusCode != 429)) {
            throw JsonParser.parse<RequestException>(text)
        }

        // If we are being asked to parse the text into
        //a raw JsonElement we want to cover this.
        // There are several cases where this is useful.
        if(JsonElement::class.java.isAssignableFrom(javaType)) return JsonTreeParser(text).readFully()

        // Deserialize using kotlinx.serialization
        return JsonParser.parse(kotlinType.serializer(), text)
    }

    @Suppress("UNCHECKED_CAST")
    override fun write(data: Any): OutgoingContent {
        val text = when(data) {
            // Empty content returns itself
            is EmptyContent -> return data

            // MultiPartFormDataContent is used in for sending messages
            //with file attachments, and it should be handled.
            is MultiPartFormDataContent -> return data

            // Strings should just be sent as is.
            // Note that this option is used only in rare cases
            //where it might be more optimal to use a preexisting
            //value, such as a cached JSON string, or a constant.
            is String -> data

            // Like the read function, we can stringify
            //and write certain JsonElement types
            is JsonObject, is JsonArray -> (data as JsonElement).stringify()
            else -> JsonParser.stringify(data::class.serializer() as KSerializer<Any>, data)
        }

        // The text is packaged into content with the requester JSON content-type.
        return TextContent(text, DiscordRequester.JsonContentType)
    }
}
