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
package me.kgustave.dkt.test

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.util.concurrent.TimeUnit

abstract class CoroutineTestBase {
    private lateinit var runContext: ExecutorCoroutineDispatcher
    private var testNumber = 0

    @BeforeEach
    fun initNewContext() {
        testNumber++
        runContext = newSingleThreadContext("Test Context Thread ($testNumber)")
    }

    protected fun runTest(block: suspend CoroutineScope.() -> Unit) {
        val job = GlobalScope.launch(runContext, start = LAZY, block = block)

        runBlocking { job.join() }
    }

    protected fun runTestWithTimeout(time: Long, unit: TimeUnit, block: suspend CoroutineScope.() -> Unit) {
        val job = GlobalScope.launch(runContext, start = LAZY) {
            withTimeoutOrNull(time, unit, block) ?: fail {
                "Test timed out!"
            }
        }

        runBlocking { job.join() }
    }

    @AfterEach
    fun destroyOldContext() {
        runContext.close()
    }
}
