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

import me.kgustave.dkt.internal.websocket.Payload
import me.kgustave.dkt.util.createLogger
import java.util.*

internal class EventCache {
    private val map = hashMapOf<Type, MutableMap<Long, Queue<CacheEntry>>>()

    @Synchronized fun cache(type: Type, id: Long, payload: Payload, callback: (event: Payload) -> Unit) {
        val ofType = map.computeIfAbsent(type) { hashMapOf() }
        val ofId = ofType.computeIfAbsent(id) { LinkedList() }
        ofId += CacheEntry(payload, callback)
    }

    @Synchronized fun play(type: Type, id: Long) {
        map[type]?.remove(id)?.forEach {
            Log.debug("Replaying events of type $type for ID: $id")
            it.callback()
        }
    }

    @Synchronized fun clear(type: Type, id: Long) {
        val ofType = map[type]?.remove(id) ?: return
        Log.debug("Clearing ${ofType.size} events for ID: $id")
    }

    @Synchronized fun count(): Int {
        return map.asSequence().sumBy { type ->
            type.value.values.asSequence().sumBy { id -> id.size }
        }
    }

    enum class Type { USER, MEMBER, GUILD, CHANNEL, ROLE }

    private data class CacheEntry(private val payload: Payload, val callback: (event: Payload) -> Unit) {
        fun callback() = callback(payload)
    }

    companion object {
        val Log = createLogger(EventCache::class)
    }
}
