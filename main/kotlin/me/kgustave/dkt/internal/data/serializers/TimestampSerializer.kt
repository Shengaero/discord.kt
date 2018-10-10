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

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import java.time.OffsetDateTime

object TimestampSerializer: KSerializer<OffsetDateTime> {
    override val descriptor = SerialClassDescImpl("io.ktor.util.date.GMTDate")
    override fun deserialize(input: Decoder): OffsetDateTime {
        require(input is CompositeDecoder)
        val index = input.decodeElementIndex(descriptor)
        val annotations = descriptor.getElementAnnotations(index)
        val timeFormat = annotations.asSequence().mapNotNull { it as? SerialTimeFormat }.firstOrNull()
        requireNotNull(timeFormat) { "GMTDate deserialization requires @SerialTimeFormat" }
        val raw = input.decodeString()
        return OffsetDateTime.parse(raw, timeFormat.kind.formatter)
    }

    override fun serialize(output: Encoder, obj: OffsetDateTime) {
        throw UnsupportedOperationException("Serializing OffsetDateTime is not supported!")
    }
}
