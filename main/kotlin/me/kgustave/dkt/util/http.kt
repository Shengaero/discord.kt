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
@file:JvmName("HttpUtil")
package me.kgustave.dkt.util

import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Job
import kotlinx.coroutines.io.jvm.javaio.toInputStream
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
    content.toInputStream(coroutineContext[Job]).use {
        val input = if(isGzip) GZIPInputStream(it) else it
        return input.use { input.reader(charset).use { reader -> reader.readText() } }
    }
}

@Deprecated("replaced with extension",
    ReplaceWith("response.readBody(isGzip)", imports = ["me.kgustave.dkt.util"]))
fun readHttpResponseBody(response: HttpResponse, isGzip: Boolean = response.isGzip()): String =
    response.readBody(isGzip)

@Deprecated("unused, scheduled for removal without replacement", level = DeprecationLevel.ERROR)
fun readGzipText(text: String, charset: Charset = Charsets.UTF_8): String {
    return text.byteInputStream(charset).use {
        GZIPInputStream(it).use { gzip ->
            gzip.reader(charset).use { reader -> reader.readText() }
        }
    }
}
