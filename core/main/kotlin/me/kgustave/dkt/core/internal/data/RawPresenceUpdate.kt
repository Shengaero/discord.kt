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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import me.kgustave.dkt.core.entities.Activity
import me.kgustave.dkt.core.internal.data.serializers.ActivitySerializer
import me.kgustave.dkt.core.entities.OnlineStatus
import me.kgustave.dkt.core.internal.data.serializers.JsonElementSerializer
import me.kgustave.dkt.core.internal.data.serializers.SnowflakeArraySerializer
import me.kgustave.dkt.core.internal.data.serializers.SnowflakeSerializer

@Serializable
internal data class RawPresenceUpdate(
    @Serializable(JsonElementSerializer::class)
    val user: JsonObject,

    @Optional
    @Serializable(SnowflakeArraySerializer::class)
    val roles: List<Long> = emptyList(),

    @Serializable(ActivitySerializer::class)
    val game: Activity?,

    @Optional
    @Serializable(SnowflakeSerializer::class)
    val guildId: Long? = null,

    @Serializable(OnlineStatus.Companion::class)
    val status: OnlineStatus
)
