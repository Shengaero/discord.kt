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

private typealias ByNameFunction<T> = (entity: T) -> String

internal abstract class AbstractCacheImpl<T>(
    private val map: MutableMap<Long, T>,
    private val byName: ByNameFunction<T>? = null
): Cache<T>, MutableMap<Long, T> by map {
    constructor(byName: ByNameFunction<T>? = null): this(hashMapOf(), byName)

    override fun getByName(name: String, ignoreCase: Boolean): List<T> {
        if(byName == null) throw UnsupportedOperationException("Getting entities by name is not supported!")
        val returns = arrayListOf<T>()
        for(entity in this) {
            if(name.equals(byName.invoke(entity), ignoreCase)) {
                returns += entity
            }
        }
        return returns
    }
}

internal open class SnowflakeCacheImpl<S: Snowflake>
constructor(byName: ByNameFunction<S>? = null): SnowflakeCache<S>, AbstractCacheImpl<S>(byName)
