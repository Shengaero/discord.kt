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
@file:JvmName("JsonUtil__Internal")
package me.kgustave.dkt.core.internal.util

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Mapper
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import me.kgustave.dkt.core.internal.DktInternal

internal val JsonParser = JSON(strictMode = false)

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun JsonBuilder.set(key: String, value: Any?) { key to jsonElementOf(value) }

internal fun Array<*>.toJsonArray(): JsonArray = jsonArrayOf(*this)
internal fun Collection<*>.toJsonArray(): JsonArray =
    jsonArray { for(value in this@toJsonArray) +jsonElementOf(value) }

internal fun Map<String, *>.toJsonObject(): JsonObject =
    json { for((key, value) in this@toJsonObject) key to jsonElementOf(value) }

internal fun jsonObjectOf(vararg pairs: Pair<String, Any?>): JsonObject =
    json { for((key, value) in pairs) key to jsonElementOf(value) }

internal fun jsonArrayOf(vararg elements: Any?): JsonArray = JsonArray(elements.map(::jsonElementOf))

@UseExperimental(ImplicitReflectionSerializer::class)
internal fun jsonElementOf(element: Any?): JsonElement {
    return when(element) {
        null -> JsonNull
        is JsonElement -> element
        is Number -> JsonPrimitive(element)
        is Boolean -> JsonPrimitive(element)
        is String -> JsonPrimitive(element)
        is Array<*> -> element.toJsonArray()
        is Collection<*> -> element.toJsonArray()
        is Map<*, *> -> jsonObjectFromInvariantMap(element)
        else ->
            @Suppress("UNCHECKED_CAST")
            Mapper.mapNullable(element::class.serializer() as KSerializer<Any>, element).toJsonObject()
    }
}

// More efficient stringify functions for JsonObject and JsonArray
// Normally the toString methods create a new StringBuilder ever
//time they call toString on an inner object or array. These methods
//use only the top level buffer.

@DktInternal fun JsonElement.stringify(): String = buildString { writeElement(this@stringify) }

private fun jsonObjectFromInvariantMap(map: Map<*, *>): JsonObject {
    if(map.isEmpty()) return jsonObjectOf()
    val mapped = hashMapOf<String, JsonElement>()
    for((key, value) in map) {
        val keyString = (key as? String) ?: key.toString()
        mapped[keyString] = jsonElementOf(value)
    }
    return JsonObject(mapped)
}

private fun Appendable.writeElement(element: JsonElement) {
    when(element) {
        is JsonObject -> writeObj(element)
        is JsonArray -> writeArray(element)
        else -> append(element.toString())
    }
}

private fun Appendable.writeObj(obj: JsonObject) {
    append('{') // prefix
    var separate = false
    for((k, v) in obj) {
        if(separate) append(',') else separate = true
        append(""""$k":""")
        writeElement(v)
    }
    append('}') // postfix
}

private fun Appendable.writeArray(array: JsonArray) {
    append('[') // prefix
    var separate = false
    for(v in array) {
        if(separate) append(',') else separate = true
        writeElement(v)
    }
    append(']') // postfix
}
