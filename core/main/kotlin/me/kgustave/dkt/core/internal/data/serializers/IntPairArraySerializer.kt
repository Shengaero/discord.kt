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

object IntPairArraySerializer: KSerializer<Pair<Int, Int>> {
    override val descriptor: SerialDescriptor = Int.serializer().list.descriptor

    override fun deserialize(input: Decoder): Pair<Int, Int> {
        @Suppress("NAME_SHADOWING")
        val input = input.beginStructure(descriptor, Int.serializer())
        val first = input.decodeIntElement(descriptor, 1)
        val second = input.decodeIntElement(descriptor, 2)
        input.endStructure(descriptor)
        return first to second
    }

    override fun serialize(output: Encoder, obj: Pair<Int, Int>) {
        @Suppress("NAME_SHADOWING")
        val output = output.beginCollection(descriptor, 2, Int.serializer())
        output.encodeIntElement(descriptor, 1, obj.first)
        output.encodeIntElement(descriptor, 2, obj.second)
        output.endStructure(descriptor)
    }
}
