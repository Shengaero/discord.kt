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

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializer(forClass = JsonElement::class)
object JsonElementSerializer {
    override fun deserialize(input: Decoder): JsonElement {
        check(input is JSON.JsonInput)
        return input.readAsTree()
    }

    override fun serialize(output: Encoder, obj: JsonElement) {
        check(output is JSON.JsonOutput)
        output.writeTree(obj)
    }
}

//@Serializer(forClass = JsonArray::class)
//object JsonArraySerializer {
//    override fun deserialize(input: Decoder): JsonArray = JsonElementSerializer.deserialize(input).jsonArray
//
//    override fun serialize(output: Encoder, obj: JsonArray) = JsonElementSerializer.serialize(output, obj)
//}

//@Serializer(forClass = JsonObject::class)
//object JsonObjectSerializer {
//    override fun deserialize(input: Decoder): JsonObject = JsonElementSerializer.deserialize(input).jsonObject
//
//    override fun serialize(output: Encoder, obj: JsonObject) = JsonElementSerializer.serialize(output, obj)
//}
