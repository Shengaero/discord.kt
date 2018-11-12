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
@file:JvmName("CoroutinesUtil")
package me.kgustave.dkt.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Flushes all queued elements from the receiver [channel][ReceiveChannel].
 *
 * Returns `false` if and only if the channel is [cancelled][ReceiveChannel.cancel]
 * before it has been emptied.
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> ReceiveChannel<T>.flush(): Boolean {
    while(!isEmpty) runCatching { poll() }.getOrElse { return false } ?: break
    return true
}
