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

interface SnowflakeCache<S: Snowflake>: Map<Long, S>, Collection<S> {
    override fun contains(element: S): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<S>): Boolean = values.containsAll(elements)
    override fun iterator(): Iterator<S> = values.iterator()
}

interface OrderedSnowflakeCache<S>: SnowflakeCache<S> where S: Snowflake, S: Comparable<S>

interface NamedSnowflakeCache<S: Snowflake>: SnowflakeCache<S> {
    fun getByName(name: String, ignoreCase: Boolean = false): S?
}
