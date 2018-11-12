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

import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.PermissionHolder
import me.kgustave.dkt.entities.PermissionOverride
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.util.delegates.weak

@DktInternal
class PermissionOverrideImpl internal constructor(
    guildChannel: AbstractGuildChannelImpl,
    internal val id: Long,
    private val holder: PermissionHolder
): PermissionOverride {
    override val channel by weak(guildChannel)

    override var rawAllowed = 0L
    override var rawDenied = 0L
    override val rawInherited get() = (rawAllowed or rawDenied).inv()
    override val allowed get() = Permission.setOf(rawAllowed).toList()
    override val denied get() = Permission.setOf(rawDenied).toList()
    override val inherited get() = Permission.setOf(rawInherited).toList()

    override val bot: DiscordBotImpl get() = channel.bot
    override val member: MemberImpl? get() = holder as? MemberImpl
    override val role: RoleImpl? get() = holder as? RoleImpl

    override fun delete(): RestPromise<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
