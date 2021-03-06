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

import me.kgustave.dkt.core.entities.GuildEmote
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.util.delegates.weak

@DktInternal
@Suppress("DEPRECATION")
@Deprecated("GuildEmote is now deprecated in favor of GuildEmoji")
class GuildEmoteImpl
internal constructor(id: Long, bot: DiscordBotImpl, guild: GuildImpl): GuildEmote, EmoteImpl(id, bot) {
    internal constructor(id: Long, guild: GuildImpl): this(id, guild.bot, guild)

    override lateinit var user: UserImpl
    override val guild: GuildImpl by weak(guild)
    override val roles = arrayListOf<RoleImpl>()
}
