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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// More efficient stringify functions for JsonObject and JsonArray
// Normally the toString methods create a new StringBuilder ever
//time they call toString on an inner object or array. These methods
//use only the top level buffer.

fun JsonElement.stringify() = when(this) {
    is JsonObject -> stringify()
    is JsonArray -> stringify()
    else -> throw UnsupportedOperationException("${this::class} is not supported by this function!")
}

fun JsonObject.stringify(): String = buildString { writeObj(this@stringify) }
fun JsonArray.stringify(): String = buildString { writeArray(this@stringify) }

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
