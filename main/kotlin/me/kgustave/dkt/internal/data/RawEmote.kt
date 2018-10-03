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
import me.kgustave.dkt.internal.data.serializers.SnowflakeSerializer

@Serializable
internal data class RawEmote(
    @Serializable(SnowflakeSerializer::class)
    val id: Long?,
    val name: String,
    @Optional val roles: List<RawRole> = emptyList(),
    @Optional val user: RawUser? = null,
    @Optional @SerialName("require_colons") val requireColons: Boolean = true,
    @Optional val managed: Boolean = true,
    @Optional val animated: Boolean = true
)
