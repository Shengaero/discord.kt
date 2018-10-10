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
import me.kgustave.dkt.entities.cache.NamedSnowflakeCache

internal open class SnowflakeCacheImpl<S: Snowflake>(
    private val map: MutableMap<Long, S>,
    private val byName: ((entity: S) -> String)? = null
): NamedSnowflakeCache<S>, MutableMap<Long, S> by map {
    constructor(byName: ((entity: S) -> String)? = null): this(hashMapOf(), byName)

    override fun getByName(name: String, ignoreCase: Boolean): List<S> {
        if(byName == null) throw UnsupportedOperationException("Getting entities by name is not supported!")
        val returns = arrayListOf<S>()
        for(entity in this) {
            if(name.equals(byName.invoke(entity), ignoreCase)) {
                returns += entity
            }
        }
        return returns
    }
}
