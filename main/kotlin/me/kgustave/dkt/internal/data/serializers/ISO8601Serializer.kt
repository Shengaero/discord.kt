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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import me.kgustave.dkt.util.parseOffsetDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal object ISO8601Serializer: KSerializer<OffsetDateTime> {
    override val descriptor = SerialClassDescImpl("java.time.OffsetDateTime")
    override fun deserialize(input: Decoder): OffsetDateTime {
        val raw = input.decodeString()
        return parseOffsetDateTime(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    override fun serialize(output: Encoder, obj: OffsetDateTime) {
        output.encodeString(obj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
}
