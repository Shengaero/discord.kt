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
package me.kgustave.dkt.internal.data.serializers

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import me.kgustave.dkt.util.toJsonObject

@Serializer(forClass = JsonElement::class)
object JsonElementSerializer {
    private val jsonObjectSerializer by lazy { (String.serializer() to this).map }
    private val jsonArraySerializer by lazy { this.list }

    override fun deserialize(input: Decoder): JsonElement {
        return when(input) {
            is JSON.JsonInput -> input.readAsTree()
            is Mapper.InMapper -> input.map.toJsonObject()
            is Mapper.InNullableMapper -> input.map.toJsonObject()
            else -> throw UnsupportedOperationException(
                "Cannot deserialize input type: ${input::class}")
        }
    }

    override fun serialize(output: Encoder, obj: JsonElement) {
        when(output) {
            // Fast path
            is JSON.JsonOutput -> return output.writeTree(obj)

            // Slow path
            else -> return when(obj) {
                is JsonNull -> output.encodeNull()
                is JsonLiteral -> {
                    obj.longOrNull?.let { return output.encodeLong(it) }
                    obj.intOrNull?.let { return output.encodeInt(it) }
                    obj.doubleOrNull?.let { return output.encodeDouble(it) }
                    obj.floatOrNull?.let { return output.encodeFloat(it) }
                    output.encodeString(obj.content)
                }
                is JsonObject -> output.encode(jsonObjectSerializer, obj)
                is JsonArray -> output.encode(jsonArraySerializer, obj)
            }
        }
    }

    init {
        registerSerializer("kotlinx.serialization.json.JsonElement", this)
        registerSerializer("kotlinx.serialization.json.JsonNull", this)
        registerSerializer("kotlinx.serialization.json.JsonLiteral", this)
        registerSerializer("kotlinx.serialization.json.JsonObject", this)
        registerSerializer("kotlinx.serialization.json.JsonArray", this)
    }
}
