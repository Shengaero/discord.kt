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
package me.kgustave.dkt.core.internal.data.serializers

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonPrimitive

object SnowflakeSerializer: KSerializer<Long> by Long.serializer() {
    override fun deserialize(input: Decoder): Long = input.decodeString().toLong()

    override fun serialize(output: Encoder, obj: Long) {
        val json = output as JSON.JsonOutput
        json.writeTree(JsonPrimitive("$obj"))
    }
}

object SnowflakeArraySerializer: KSerializer<List<Long>> by SnowflakeSerializer.list
