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
package me.kgustave.dkt.internal.entities

import me.kgustave.dkt.entities.Emoji
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.util.delegates.weak

@DktInternal
open class EmojiImpl internal constructor(bot: DiscordBotImpl, override val id: Long?): Emoji {
    override val bot: DiscordBotImpl by weak(bot)

    final override lateinit var name: String
    override lateinit var imageUrl: String
    override var isAnimated: Boolean = false
    override var isManaged: Boolean = false

    internal constructor(bot: DiscordBotImpl, name: String): this(bot, null) {
        this.name = name
    }

    override val guild: GuildImpl? get() = null
    override val roles: List<RoleImpl>? get() = null
    override val user: UserImpl? get() = null
}
