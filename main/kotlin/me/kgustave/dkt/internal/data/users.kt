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
import kotlinx.serialization.Serializable
import me.kgustave.dkt.internal.data.serializers.SnowflakeSerializer

sealed class RawUserData {
    @Serializable(SnowflakeSerializer::class)
    abstract val id: Long

    abstract val username: String

    abstract val discriminator: String

    abstract val avatar: String?

    @Optional
    abstract val bot: Boolean
}

@Serializable
internal data class RawUser(
    @Serializable(SnowflakeSerializer::class)
    override val id: Long,

    override val username: String,

    override val discriminator: String,

    override val avatar: String?,

    @Optional
    override val bot: Boolean = false
): RawUserData()

@Serializable
internal data class RawSelfUser(
    @Serializable(SnowflakeSerializer::class)
    override val id: Long,

    override val username: String,

    override val discriminator: String,

    override val avatar: String?,

    @Optional
    override val bot: Boolean = true
): RawUserData()
