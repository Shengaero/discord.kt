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
package me.kgustave.dkt.entities.cache

import me.kgustave.dkt.entities.Snowflake
import java.lang.UnsupportedOperationException

// TODO possibly add type variance to values?

interface Cache<V>: Collection<V> {
    fun getById(id: Long): V?

    operator fun get(key: Long): V? = getById(key)
    operator fun contains(key: Long): Boolean

    fun getByName(name: String, ignoreCase: Boolean = false): List<V> {
        throw UnsupportedOperationException("Getting cached entities by name is not supported by this cache!")
    }

    @Deprecated("replace with normal stdlib extension",
        ReplaceWith("toList<V>()", imports = ["kotlin.collections.toList"]))
    fun toList(): List<V> = toList<V>()
}

interface SnowflakeCache<S: Snowflake>: Cache<S>
