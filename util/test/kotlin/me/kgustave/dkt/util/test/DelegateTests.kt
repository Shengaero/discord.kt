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
@file:Suppress("UNUSED_VARIABLE", "UNCHECKED_CAST")
package me.kgustave.dkt.util.test

import me.kgustave.dkt.util.delegates.cleaningRef
import me.kgustave.dkt.util.delegates.weak
import org.junit.jupiter.api.Test
import kotlin.test.*

class DelegateTests {
    @Test fun `Test Weak Delegate`() {
        val delegate = weak<String?>(null)
        var str by delegate

        assertTrue(delegate.isSetToNull)
        assertNull(str)

        str = "not null"
        assertFalse(delegate.isSetToNull)
        assertEquals("not null", str)

        // clear this manually
        delegate.ref.clear()
        assertFails { val unused = str }
    }

    @Test fun `Test Cleaning Ref Delegate`() {
        var number by cleaningRef(3)

        assertEquals(3, number)
        assertNull(number)

        number = 5

        assertEquals(5, number)
        assertNull(number)
    }
}
