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
@file:Suppress("unused")
package me.kgustave.dkt.internal.data

import io.ktor.util.date.GMTDate
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.kgustave.dkt.internal.data.serializers.SerialTimeFormat
import me.kgustave.dkt.internal.data.serializers.SnowflakeSerializer
import me.kgustave.dkt.internal.data.serializers.TimestampSerializer

@Serializable
internal class RawGuild(
    @Serializable(SnowflakeSerializer::class)
    val id: Long,

    val name: String,

    val icon: String?,

    val splash: String?,

    @Optional
    val owner: Boolean = false,

    @SerialName("owner_id")
    @Serializable(SnowflakeSerializer::class)
    val ownerId: Long,

    @Optional
    val permissions: Int? = null,

    val region: String,

    @SerialName("afk_channel_id")
    @Serializable(SnowflakeSerializer::class)
    val afkChannelId: Long?,

    @SerialName("afk_timeout")
    val afkTimeout: Long,

    @Optional
    @SerialName("embed_enabled")
    val embedEnabled: Boolean = false,

    @Optional
    @SerialName("embed_channel_id")
    @Serializable(SnowflakeSerializer::class)
    val embedChannelId: Long? = null,

    @SerialName("verification_level")
    val verificationLevel: Int,

    @SerialName("default_message_notifications")
    val defaultMessageNotifications: Int,

    @SerialName("explicit_content_filter")
    val explicitContentFilter: Int,

    val roles: List<RawRole>,

    val emojis: List<RawEmote>,

    val features: List<String>,

    @Optional
    @SerialName("application_id")
    @Serializable(SnowflakeSerializer::class)
    val applicationId: Long? = null,

    @Optional
    @SerialName("widget_enabled")
    val widgetEnabled: Boolean = false,

    @Optional
    @SerialName("widget_channel_id")
    @Serializable(SnowflakeSerializer::class)
    val widgetChannelId: Long? = null,

    @Optional
    @SerialName("system_channel_id")
    @Serializable(SnowflakeSerializer::class)
    val systemChannelId: Long? = null,

    @Optional val unavailable: Boolean = true,

    @Optional
    @SerialName("member_count")
    val memberCount: Int? = null,

    @Optional
    @SerialName("joined_at")
    @Serializable(TimestampSerializer::class)
    @SerialTimeFormat(SerialTimeFormat.Kind.ISO_OFFSET_DATE_TIME)
    val joinedAt: GMTDate? = null
)
