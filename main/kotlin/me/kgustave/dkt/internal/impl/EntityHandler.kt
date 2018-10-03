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
package me.kgustave.dkt.internal.impl

import me.kgustave.dkt.entities.User
import me.kgustave.dkt.internal.data.RawUser

internal class EntityHandler(private val discord: DiscordBotImpl) {
    fun handleUser(raw: RawUser): UserImpl {
        return UserImpl(discord, raw)
    }

    fun handleSelfUser(raw: RawUser): User {
        val self = SelfUserImpl(discord, raw)
        discord.self = self
        return self
    }
}
