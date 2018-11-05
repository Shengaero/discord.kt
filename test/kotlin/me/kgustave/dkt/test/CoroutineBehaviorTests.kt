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
package me.kgustave.dkt.test

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.test.assertEquals

class CoroutineBehaviorTests: CoroutineTestBase() {
    @Test fun `Test startCoroutine Behavior`() = runTest {
        val channel = Channel<Int>(Channel.RENDEZVOUS)
        val completion = CompletableDeferred<Int>()
        val continuation = Continuation<Int>(Dispatchers.Default) { result ->
            result
                .onSuccess { completion.complete(it) }
                .onFailure { completion.completeExceptionally(it) }
        }

        suspend { channel.receive() }.startCoroutine(continuation)

        withTimeout(2000L) {
            launch(Dispatchers.Default) { channel.send(2) }
            assertEquals(2, completion.await())
        }
    }
}
