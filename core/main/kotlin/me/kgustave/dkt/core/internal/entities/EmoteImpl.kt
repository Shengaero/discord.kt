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
package me.kgustave.dkt.core.internal.entities

import me.kgustave.dkt.core.entities.Emote
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.util.delegates.weak

@DktInternal
@Suppress("DEPRECATION")
@Deprecated("Emote is now deprecated in favor of Emoji")
open class EmoteImpl internal constructor(override val id: Long, bot: DiscordBotImpl): Emote {
    override val bot: DiscordBotImpl by weak(bot)

    override lateinit var name: String
    override lateinit var imageUrl: String
    override var isAnimated: Boolean = false
    override var isManaged: Boolean = false

    override val guild: GuildImpl? get() = null
    override val roles: List<RoleImpl>? get() = null
    override val user: UserImpl? get() = null
}