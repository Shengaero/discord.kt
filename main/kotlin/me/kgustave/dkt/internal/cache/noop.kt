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
@file:Suppress("UNCHECKED_CAST")
package me.kgustave.dkt.internal.cache

import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.entities.cache.NamedSnowflakeCache
import me.kgustave.dkt.entities.cache.SnowflakeCache

private object NoopSnowflakeCache: SnowflakeCache<Snowflake>, NamedSnowflakeCache<Snowflake> {
    override val entries = emptySet<Map.Entry<Long, Snowflake>>()
    override val keys = emptySet<Long>()
    override val size = 0
    override val values = emptyList<Snowflake>()

    override fun containsKey(key: Long): Boolean = false
    override fun containsValue(value: Snowflake): Boolean = false
    override fun get(key: Long): Snowflake? = null
    override fun isEmpty(): Boolean = true

    override fun getByName(name: String, ignoreCase: Boolean): Snowflake? = null
}

internal fun noopUserSnowflakeCache() = NoopSnowflakeCache as NamedSnowflakeCache<User>
internal fun noopGuildSnowflakeCache() = NoopSnowflakeCache as NamedSnowflakeCache<Guild>
