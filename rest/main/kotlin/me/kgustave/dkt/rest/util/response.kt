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
@file:JvmName("ResponseUtil")
package me.kgustave.dkt.rest.util

import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.util.cio.toByteArray
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

/**
 * Returns `true` if this response uses gzip
 */
fun HttpResponse.isGzip(): Boolean = headers[HttpHeaders.ContentEncoding]?.toLowerCase() == "gzip"

/**
 * Reads the response body content of the [HttpResponse], returning it as a string.
 */
fun HttpResponse.readBody(isGzip: Boolean = isGzip(), charset: Charset = Charsets.UTF_8): String {
    val ba = runBlocking { content.toByteArray() }
    return ByteArrayInputStream(ba).use { input ->
        if(isGzip) {
            GZIPInputStream(input).reader(charset).use { reader ->
                reader.readText()
            }
        } else {
            input.reader(charset).use { reader ->
                reader.readText()
            }
        }
    }
}
