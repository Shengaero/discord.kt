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

import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.internal.util.canInteract

internal class MemberImpl(override val guild: GuildImpl, override val user: UserImpl): Member {
    override var nickname: String? = null
    override var rawPermissions: Long = 0L
    override lateinit var voiceState: GuildVoiceStateImpl

    override val roles = arrayListOf<RoleImpl>()

    override val bot get() = guild.bot
    override val permissions get() = Permission.setOf(rawPermissions).toList()

    internal fun voiceStateIsInit(): Boolean = ::voiceState.isInitialized

    override fun canInteractWith(member: Member): Boolean = canInteract(this, member)
    override fun canInteractWith(role: Role): Boolean = canInteract(this, role)

    override fun toString(): String = Snowflake.toString("M", user)
    override fun hashCode(): Int = (guild.id + user.id).hashCode()
    override fun equals(other: Any?): Boolean {
        if(other !is Member) return false
        if(other === this) return true

        return user == other.user && guild == other.guild
    }
}
