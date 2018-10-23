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

// FIXME simplify type hierarchy for caches
// TODO possibly add type variance to values?

interface Cache<V>: Map<Long, V>, Collection<V> {
    override fun contains(element: V): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<V>): Boolean = values.containsAll(elements)
    override fun iterator(): Iterator<V> = values.iterator()

    fun toList(): List<V>

    fun getById(id: Long): V? = this[id]

    fun getByName(name: String, ignoreCase: Boolean = false): List<V> {
        throw UnsupportedOperationException("Getting cached entities by name is not supported by this cache!")
    }
}

interface SnowflakeCache<S: Snowflake>: Cache<S>
