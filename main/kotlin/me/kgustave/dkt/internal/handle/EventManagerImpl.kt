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
package me.kgustave.dkt.internal.handle

import me.kgustave.dkt.events.Event
import me.kgustave.dkt.handle.EventManager

// TODO Standard EventManager Implementation
internal class EventManagerImpl: EventManager {
    override val listeners = mutableListOf<Any>()

    override fun dispatch(event: Event) {
        // TODO
    }

    override fun addListener(listener: Any) {
        // TODO
    }

    override fun removeListener(listener: Any) {
        // TODO
    }
}
