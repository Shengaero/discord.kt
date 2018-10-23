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
package me.kgustave.dkt.test

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.json
import me.kgustave.dkt.util.delegates.cleaningRef
import me.kgustave.dkt.util.delegates.weak
import me.kgustave.dkt.util.stringify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

class UtilityTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    inner class WeakDelegateTests {
        private val delegate = weak<String?>(null)
        private var str by delegate

        @Test fun `Test Weak Delegate Nullability`() {
            assertTrue(delegate.isSetToNull)
            assertNull(str)

            str = "not null"
            assertFalse(delegate.isSetToNull)
            assertEquals("not null", str)

            // clear this manually
            delegate.ref.clear()
            assertFails { val unused = str }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    inner class CleaningRefTests {
        private var number by cleaningRef<Int>()

        @Test fun `Test Cleaning Reference Is Null After Getter Is Called`() {
            number = 5
            assertEquals(5, number)
            assertNull(number)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    inner class JsonTests {
        @Test fun `Test Json Stringify`() {
            assertEquals("""{"foo":"bar","baz":"biz"}""", json {
                "foo" to "bar"
                "baz" to "biz"
            }.stringify())
            assertEquals("""["1",2,"three"]""", JsonArray(listOf(
                JsonPrimitive("1"),
                JsonPrimitive(2),
                JsonPrimitive("three")
            )).stringify())
        }
    }
}
