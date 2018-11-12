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
package me.kgustave.dkt.core.handle

import kotlinx.coroutines.CompletableDeferred
import me.kgustave.dkt.core.DiscordBot
import me.kgustave.dkt.core.events.Event
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl

@ExperimentalEventListeners
inline fun <reified E: Event> DiscordBot.on(crossinline listener: (event: E) -> Unit) {
    addListener(object: EventListener {
        override fun on(event: Event) {
            if(event is E) {
                listener(event)
            }
        }
    })
}

@ExperimentalEventListeners
suspend inline fun <reified E: Event> DiscordBot.receive(crossinline condition: (event: E) -> Boolean = { true }): E {
    this as DiscordBotImpl
    val completion = CompletableDeferred<E>()
    val awaiting = object: EventListener {
        override fun on(event: Event) {
            if(event is E && condition(event)) {
                completion.complete(event)
            }
        }
    }
    addListener(awaiting)
    val completed = completion.await()
    removeListener(awaiting)
    return completed
}
