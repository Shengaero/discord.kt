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
package opus.test

import opus.ExperimentalOpus
import opus.Opus
import opus.Opus.Constants
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@UseExperimental(ExperimentalOpus::class)
class OpusTests {
    @BeforeEach fun `Initialize Opus`() {
        Opus.initialize()
    }

    @Test fun `Test opus_str_error`() {
        assertEquals("buffer too small", Opus.strError(Constants.BUFFER_TOO_SMALL))
        assertEquals("memory allocation failed", Opus.strError(Constants.MEMORY_ALLOCATION_FAILED))
    }
}
