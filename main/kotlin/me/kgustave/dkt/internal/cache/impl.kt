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
package me.kgustave.dkt.internal.cache

import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.cache.Cache
import me.kgustave.dkt.entities.cache.SnowflakeCache
import java.util.Spliterators
import java.util.Spliterator
import java.util.stream.Stream

private typealias ByNameFunction<T> = (entity: T) -> String

internal abstract class AbstractCacheImpl<T>(
    private val map: MutableMap<Long, T>,
    private val byName: ByNameFunction<T>? = null
): Cache<T>, MutableMap<Long, T> by map {
    constructor(byName: ByNameFunction<T>? = null): this(hashMapOf(), byName)

    override fun toList(): List<T> = values.toList()

    override fun getByName(name: String, ignoreCase: Boolean): List<T> {
        if(byName == null)
            throw UnsupportedOperationException("Getting entities by name is not supported by this cache!")
        if(name.isBlank())
            return emptyList()
        val list = arrayListOf<T>()
        for(entity in this) {
            if(name.equals(byName.invoke(entity), ignoreCase)) {
                list += entity
            }
        }
        return list
    }
}

internal open class SnowflakeCacheImpl<S: Snowflake>(byName: ByNameFunction<S>? = null):
    SnowflakeCache<S>,
    AbstractCacheImpl<S>(byName)

internal open class SortableSnowflakeCache<S>(byName: ByNameFunction<S>? = null, private val comparator: Comparator<S>):
    SnowflakeCache<S>,
    AbstractCacheImpl<S>(byName)
where S: Snowflake, S: Comparable<S> {
    override fun toList(): List<S> {
        // send to collection immediately, do not
        //sequence to avoid concurrent modification
        val list = values.toCollection(arrayListOf())
        list.sortedWith(comparator)
        return list
    }

    override fun stream(): Stream<S> = super<SnowflakeCache>.stream().sorted(comparator)
    override fun parallelStream(): Stream<S> = super<SnowflakeCache>.parallelStream().sorted(comparator)

    override fun iterator(): Iterator<S> = toList().iterator()

    override fun spliterator(): Spliterator<S> {
        return Spliterators.spliterator(iterator(), this.size.toLong(),
            Spliterator.IMMUTABLE or Spliterator.ORDERED or Spliterator.NONNULL)
    }
}
