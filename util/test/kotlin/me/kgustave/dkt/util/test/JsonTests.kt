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
package me.kgustave.dkt.util.test

import kotlinx.serialization.Serializable
import me.kgustave.dkt.util.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonTests {
    @Serializable
    data class Box(val h: Int, val w: Int)

    @Test fun `Test Json Stringify`() {
        assertEquals("""{"foo":"bar","baz":"biz"}""",
            jsonObjectOf("foo" to "bar", "baz" to "biz").stringify())
        assertEquals("""["1",2,"three"]""",
            jsonArrayOf("1", 2, "three").stringify())
    }

    @Test fun `Test Conversion To JsonObject`() {
        assertEquals("""{"h":4,"w":2}""",
            jsonElementOf(Box(4, 2)).stringify())
    }

    @Test fun `Test Conversion To JsonArray`() {
        assertEquals("""[{"h":2,"w":4},{"h":8,"w":2},{"h":6,"w":8}]""",
            listOf(Box(2, 4), Box(8, 2), Box(6, 8)).toJsonArray().stringify())
    }
}
