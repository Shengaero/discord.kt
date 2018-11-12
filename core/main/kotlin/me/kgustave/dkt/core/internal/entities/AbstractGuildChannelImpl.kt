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

import me.kgustave.dkt.core.entities.*
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.util.delegates.weak

@DktInternal
abstract class AbstractGuildChannelImpl internal constructor(override val id: Long, guild: GuildImpl): GuildChannel {
    override lateinit var name: String
    override var rawPosition: Int = -1
    override val guild: GuildImpl by weak(guild)
    override val bot: DiscordBotImpl get() = guild.bot
    override val overrides: List<PermissionOverride> get() = permissionOverrides.values.toList()

    internal val permissionOverrides = mutableMapOf<Long, PermissionOverrideImpl>()

    abstract override val parent: CategoryImpl?

    override fun permissionOverrideFor(member: Member): PermissionOverride? = permissionOverrides[member.user.id]
    override fun permissionOverrideFor(role: Role): PermissionOverride? = permissionOverrides[role.id]
}
