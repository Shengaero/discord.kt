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
import me.kgustave.dkt.core.internal.data.serializers.SnowflakeSerializer

@Serializable
internal data class RawVoiceState(
    @Optional
    @SerialName("guild_id")
    @Serializable(SnowflakeSerializer::class)
    val guildId: Long? = null,

    @SerialName("channel_id")
    @Serializable(SnowflakeSerializer::class)
    val channelId: Long?,

    @SerialName("user_id")
    @Serializable(SnowflakeSerializer::class)
    val userId: Long,

    @Optional
    val member: RawMember? = null,

    @SerialName("session_id")
    val sessionId: String,

    val deaf: Boolean,

    val mute: Boolean,

    @SerialName("self_deaf")
    val selfDeaf: Boolean,

    @SerialName("self_mute")
    val selfMute: Boolean,

    val suppress: Boolean
)
