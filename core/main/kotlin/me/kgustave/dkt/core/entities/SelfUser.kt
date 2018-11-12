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
package me.kgustave.dkt.core.entities

/**
 * Represents a the currently logged in [User].
 *
 * This is a separate interface to have documentation changes
 * as well as for any functions that would be more appropriately
 * placed here than in the [DiscordBot][me.kgustave.dkt.DiscordBot]
 * interface.
 */
interface SelfUser: User {

    /**
     * This is unsupported for [SelfUser]!
     *
     * @throws UnsupportedOperationException
     */
    override fun openPrivateChannel(): Nothing
}
