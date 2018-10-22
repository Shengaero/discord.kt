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
package me.kgustave.dkt.internal.data

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kgustave.dkt.internal.data.serializers.ISO8601Serializer
import me.kgustave.dkt.internal.data.serializers.SnowflakeSerializer
import java.time.OffsetDateTime

@Serializable
internal data class RawChannel(
    @Serializable(SnowflakeSerializer::class)
    val id: Long,

    val name: String,

    val type: Int,

    @Optional
    @SerialName("guild_id")
    @Serializable(SnowflakeSerializer::class)
    val guildId: Long? = null,

    @Optional
    val position: Int? = null,

    @Optional
    @SerialName("permission_overwrites")
    val permissionOverwrites: List<RawPermissionOverwrite> = emptyList(),

    @Optional
    val topic: String? = null,

    @Optional
    val nsfw: Boolean = false,

    @Optional
    @SerialName("last_message_id")
    @Serializable(SnowflakeSerializer::class)
    val lastMessageId: Long? = null,

    @Optional
    val bitrate: Int? = null,

    @Optional
    @SerialName("user_limit")
    val userLimit: Int? = null,

    @Optional
    @SerialName("rate_limit_per_user")
    val rateLimitPerUser: Int? = null,

    @Optional
    val recipients: List<RawUser> = emptyList(),

    @Optional
    val icon: String? = null,

    @Optional
    @SerialName("owner_id")
    @Serializable(SnowflakeSerializer::class)
    val ownerId: Long? = null,

    @Optional
    @SerialName("application_id")
    @Serializable(SnowflakeSerializer::class)
    val applicationId: Long? = null,

    @Optional
    @SerialName("parent_id")
    @Serializable(SnowflakeSerializer::class)
    val parentId: Long? = null,

    @Optional
    @SerialName("last_pin_timestamp")
    @Serializable(ISO8601Serializer::class)
    val lastPinTimestamp: OffsetDateTime? = null
)
