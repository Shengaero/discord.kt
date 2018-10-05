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

package me.kgustave.dkt.requests.serialization

import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpHeaders
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import me.kgustave.dkt.exceptions.RequestException
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.util.readHttpResponseBody
import kotlin.reflect.full.isSubclassOf

/**
 * Serializer for Discord.kt requests
 *
 * Most default ktor deserialization stuff isn't well optimized
 * or doesn't take into consideration some of the complications
 * that come with API specific behavior.
 *
 * @author Kaidan Gustave
 */
class DiscordSerializer: JsonSerializer {
    override suspend fun read(type: TypeInfo, response: HttpResponse): Any {
        val encoding = response.headers[HttpHeaders.ContentEncoding]
        // Gzip is slightly more complicated to parse using
        //ktor client at the moment (see utility function
        //in me/kgustave/dkt/util/http.kt).
        val text = if(encoding == "gzip") readHttpResponseBody(response, true) else {
            // If this isn't gzip we should go for
            //the more optimized response read.
            response.readText(response.charset() ?: Charsets.UTF_8)
        }

        // If we are being asked to parse the text into
        //a raw JsonElement we want to cover this.
        // There are several cases where this is useful.
        if(type.type.isSubclassOf(JsonElement::class)) return JsonTreeParser(text).readFully()

        // 429 should be handled separately!
        if(response.status.value !in 200 until 400 && response.status.value != 429) {
            throw JSON.nonstrict.parse<RequestException>(text)
        }

        // Deserialize using kotlinx.serialization
        return JSON.nonstrict.parse(type.type.serializer(), text)
    }

    @Suppress("UNCHECKED_CAST")
    override fun write(data: Any): OutgoingContent {
        val text = when(data) {
            // Empty content returns itself
            is EmptyContent -> return data

            // Strings should just be sent as is.
            // Note that this option is used only in rare cases
            //where it might be more optimal to use a preexisting
            //value, such as a cached JSON string, or a constant.
            is String -> data

            // Like the read function, we can stringify
            //and write certain JsonElement types
            is JsonObject,
            is JsonArray -> data.toString()
            else -> JSON.stringify(data::class.serializer() as KSerializer<Any>, data)
        }

        // The text is packaged into content with the requester JSON content-type.
        return TextContent(text, Requester.JsonContentType)
    }
}
