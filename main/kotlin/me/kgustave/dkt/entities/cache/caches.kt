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

// FIXME simplify type hierarchy for caches
// TODO possibly add type variance to values?

interface Cache<K, V>: Map<K, V>, Collection<V> {
    override fun contains(element: V): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<V>): Boolean = values.containsAll(elements)
    override fun iterator(): Iterator<V> = values.iterator()
}

interface OrderedCache<K, V: Comparable<V>>: Cache<K, V>
interface SnowflakeCache<S: Snowflake>: Cache<Long, S>
interface NamedCache<K, V>: Cache<K, V> {
    fun getByName(name: String, ignoreCase: Boolean = false): List<V>
}

interface OrderedSnowflakeCache<S>:
    SnowflakeCache<S>,
    OrderedCache<Long, S>
    where S: Snowflake, S: Comparable<S>

interface NamedSnowflakeCache<S: Snowflake>:
    SnowflakeCache<S>,
    NamedCache<Long, S>

interface NamedOrderedCache<K, V: Comparable<V>>:
    NamedCache<K, V>,
    OrderedCache<K, V>

interface NamedOrderedSnowflakeCache<S>:
    OrderedSnowflakeCache<S>,
    NamedSnowflakeCache<S>,
    NamedOrderedCache<Long, S>
    where S: Snowflake, S: Comparable<S>
