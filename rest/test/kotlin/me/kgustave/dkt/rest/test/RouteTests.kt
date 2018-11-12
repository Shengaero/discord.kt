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
package me.kgustave.dkt.rest.test

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import me.kgustave.dkt.rest.ExperimentalDktREST
import me.kgustave.dkt.rest.route
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@ExperimentalDktREST
class RouteTests {
    @Test fun `Test Format Route`() {
        val route = route(Post, "/routing/{id.a}/test/{id.b}", listOf("id.a"))
        val formatted = route.format(2000, 4000)

        assertEquals(route, formatted.base)
        assertEquals("/routing/2000/test/{id.b}", formatted.rateLimitedPath)
        assertEquals("/routing/2000/test/4000", formatted.path)
    }

    @Test fun `Test Route Formatting With Major Parameters Between Non-Major Parameters`() {
        val route = route(Get, "/my/{test}/route/{has.major}/parameters/{inbetween.non.major}/parameters", listOf("has.major"))
        val formatted = route.format("a", "b", "c")

        assertEquals(route, formatted.base)
        assertEquals("/my/{test}/route/b/parameters/{inbetween.non.major}/parameters", formatted.rateLimitedPath)
        assertEquals("/my/a/route/b/parameters/c/parameters", formatted.path)
    }

    @Test fun `Test Route With Query Params`() {
        val route = route(Get, "/cool/route/bro").withQuery("limit" to 20, "query" to "dogs and cats")
        assertEquals("/cool/route/bro?limit=20&query=dogs%20and%20cats", route.toString())
    }
}
