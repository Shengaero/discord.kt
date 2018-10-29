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
package me.kgustave.dkt.util

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// More efficient stringify functions for JsonObject and JsonArray
// Normally the toString methods create a new StringBuilder ever
//time they call toString on an inner object or array. These methods
//use only the top level buffer.

internal val JsonParser = JSON(strictMode = false)

internal fun Any.toJsonObject(): JsonObject {
    val m = Mapper.OutNullableMapper()
    @Suppress("UNCHECKED_CAST")
    m.encode(this::class.serializer() as KSerializer<Any>, this)
    return m.map.toJsonObject()
}

operator fun JsonBuilder.set(key: String, value: Any?) {
    key to jsonElementOf(value)
}

fun Array<*>.toJsonArray(): JsonArray = jsonArrayOf(*this)
fun Collection<*>.toJsonArray(): JsonArray = JsonArray(map(::jsonElementOf))
fun Map<String, *>.toJsonObject(): JsonObject = JsonObject(mapValues { jsonElementOf(it.value) })

fun jsonObjectOf(vararg pairs: Pair<String, Any?>): JsonObject =
    JsonObject(pairs.associate { (key, value) -> key to jsonElementOf(value) })

fun jsonArrayOf(vararg elements: Any?): JsonArray = JsonArray(elements.map(::jsonElementOf))

fun jsonElementOf(element: Any?): JsonElement {
    return when(element) {
        null -> JsonNull
        is JsonElement -> element
        is Number -> JsonPrimitive(element)
        is Boolean -> JsonPrimitive(element)
        is String -> JsonPrimitive(element)
        is Array<*> -> element.toJsonArray()
        is Collection<*> -> element.toJsonArray()
        is Map<*, *> -> jsonObjectFromInvariantMap(element)
        else -> element.toJsonObject()
    }
}

fun JsonElement.stringify(): String {
    return when(this) {
        is JsonObject -> stringify()
        is JsonArray -> stringify()
        else -> throw UnsupportedOperationException("${this::class} is not supported by this function!")
    }
}

fun JsonObject.stringify(): String = buildString { writeObj(this@stringify) }
fun JsonArray.stringify(): String = buildString { writeArray(this@stringify) }

private fun jsonObjectFromInvariantMap(map: Map<*, *>): JsonObject {
    if(map.isEmpty()) return jsonObjectOf()
    val mapped = hashMapOf<String, JsonElement>()
    for((key, value) in map) {
        val keyString = (key as? String) ?: key.toString()
        mapped[keyString] = jsonElementOf(value)
    }
    return JsonObject(mapped)
}

private fun Appendable.writeObj(obj: JsonObject) {
    append('{') // prefix
    var separate = false
    for((k, v) in obj) {
        if(separate) append(',') else separate = true
        append(""""$k":""")
        when(v) {
            is JsonObject -> writeObj(v)
            is JsonArray -> writeArray(v)
            else -> append(v.toString())
        }
    }
    append('}') // postfix
}

private fun Appendable.writeArray(array: JsonArray) {
    append('[') // prefix
    var separate = false
    for(v in array) {
        if(separate) append(',') else separate = true
        when(v) {
            is JsonObject -> writeObj(v)
            is JsonArray -> writeArray(v)
            else -> append(v.toString())
        }
    }
    append(']') // postfix
}
