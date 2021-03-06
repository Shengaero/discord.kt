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
package me.kgustave.dkt.core.internal.data

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kgustave.dkt.core.internal.data.serializers.ISO8601Serializer
import me.kgustave.dkt.core.internal.data.serializers.SnowflakeArraySerializer
import me.kgustave.dkt.core.internal.data.serializers.SnowflakeSerializer
import java.time.OffsetDateTime

@Serializable
internal data class RawMember(
    val user: RawUser,
    @Optional
    val nick: String? = null,
    @Serializable(SnowflakeArraySerializer::class)
    val roles: List<Long>,
    @SerialName("joined_at")
    @Serializable(ISO8601Serializer::class)
    val joinedAt: OffsetDateTime,
    val deaf: Boolean,
    val mute: Boolean,

    // available GUILD_MEMBER_ADD events
    @SerialName("guild_id")
    @Serializable(SnowflakeSerializer::class)
    @Optional val guildId: Long? = null
)
