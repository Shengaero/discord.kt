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
import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.Role
import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.internal.util.canInteract
import java.awt.Color

@DktInternal
class RoleImpl(override val guild: GuildImpl, override val id: Long): Role {
    override val bot: DiscordBotImpl get() = guild.bot
    override lateinit var name: String

    override var rawPermissions = 0L
    override var rawPosition = 0

    override lateinit var color: Color
        private set

    override var colorInt = Role.DefaultColorInt
        set(value) {
            field = value
            color = Color(field)
        }

    override val permissions: List<Permission> get() = Permission.setOf(rawPermissions).toList()

    override val position: Int get() {
        if(this == guild.publicRole) return -1

        var i = guild.roleCache.size - 2
        for(role in guild.roleCache) {
            if(role == this)
                return i
            i--
        }

        error("Failed to determine role position???")
    }

    override fun canInteractWith(role: Role): Boolean = canInteract(this, role)

    override fun canInteractWith(member: Member): Boolean {
        val memberRoles = member.roles
        if(memberRoles.isEmpty()) return true
        return canInteractWith(memberRoles[0])
    }

    override fun compareTo(other: Role): Int {
        if(this === other) return 0
        if(this == other) return 0

        require(guild == other.guild) { "Cannot compare roles from different guilds!" }

        val comparison = rawPosition - other.rawPosition
        if(comparison != 0) return comparison

        return other.creationTime.compareTo(creationTime)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Role && Snowflake.equals(this, other)
    override fun toString(): String = Snowflake.toString("R", this)
}
