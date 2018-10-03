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
package me.kgustave.dkt.internal.cache

import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.cache.SnowflakeCache

open class SnowflakeCacheImpl<S: Snowflake>: SnowflakeCache<S>, MutableMap<Long, S> {
    private val cache = mutableMapOf<Long, S>()
    override val entries get() = cache.entries
    override val keys get() = cache.keys
    override val size get() = cache.size
    override val values get() = cache.values

    override fun clear() = cache.clear()
    override fun containsKey(key: Long): Boolean = cache.containsKey(key)
    override fun containsValue(value: S): Boolean = cache.containsValue(value)
    override fun get(key: Long): S? = cache[key]
    override fun isEmpty(): Boolean = cache.isEmpty()
    override fun put(key: Long, value: S): S? = cache.put(key, value)
    override fun putAll(from: Map<out Long, S>) = cache.putAll(from)
    override fun remove(key: Long): S? = cache.remove(key)
}
