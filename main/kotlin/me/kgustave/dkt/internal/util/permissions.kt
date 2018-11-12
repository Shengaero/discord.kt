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
@file:JvmName("PermissionsUtil__Internal")
@file:Suppress("LiftReturnOrAssignment", "unused")
package me.kgustave.dkt.internal.util

import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.GuildChannel
import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.Role

internal fun canInteract(member: Member, target: Member): Boolean {
    require(member.guild == target.guild) { interactionWithUncommonGuild("member") }

    when(member.guild.owner) {
        member -> return true
        target -> return false
    }

    val memberRoles = member.roles
    val targetRoles = target.roles
    return memberRoles.isNotEmpty() && (targetRoles.isEmpty() || canInteract(memberRoles[0], targetRoles[0]))
}

internal fun canInteract(member: Member, target: Role): Boolean {
    require(member.guild == target.guild) { interactionWithUncommonGuild("member", "role") }

    if(member.guild.owner == member) return true

    val memberRoles = member.roles
    return memberRoles.isNotEmpty() && canInteract(memberRoles[0], target)
}

internal fun canInteract(role: Role, target: Role): Boolean {
    require(role.guild == target.guild) { interactionWithUncommonGuild("role") }

    return target.position < role.position
}

internal fun effectivePermissionsOf(member: Member): Long {
    if(member == member.guild.owner) return Permission.AllRaw
    var permissions = member.guild.publicRole.rawPermissions
    for(role in member.roles) {
        permissions = permissions or role.rawPermissions
        if(applies(permissions, Permission.ADMINISTRATOR.raw)) {
            return Permission.AllRaw
        }
    }
    return permissions
}

internal fun effectivePermissionsOf(channel: GuildChannel, member: Member): Long {
    require(channel.guild == member.guild) { permissionsWithUncommonGuild("channel", "member") }

    if(member == member.guild.owner) return Permission.AllRaw
    var permissions = effectivePermissionsOf(member)
    if(applies(permissions, Permission.ADMINISTRATOR.raw)) return Permission.AllRaw

    val (allowed, denied) = processOverrides(channel, member)
    permissions = apply(permissions, allowed, denied)

    if(!applies(permissions, Permission.VIEW_CHANNEL.raw)) return 0

    return permissions
}

internal fun effectivePermissionsOf(channel: GuildChannel, role: Role): Long {
    require(channel.guild == role.guild) { permissionsWithUncommonGuild("channel", "role") }

    val publicRole = channel.guild.publicRole
    var permissions = role.rawPermissions or publicRole.rawPermissions

    channel.permissionOverrideFor(publicRole)?.let { publicOverride ->
        permissions = permissions and publicOverride.rawDenied.inv()
        permissions = permissions or publicOverride.rawAllowed
    }

    channel.permissionOverrideFor(role)?.let { roleOverride ->
        permissions = permissions and roleOverride.rawDenied.inv()
        permissions = permissions or roleOverride.rawAllowed
    }

    return permissions
}

internal fun explicitPermissionsOf(member: Member): Long {
    // start with the raw value of the publicRole (@everyone)
    var total = member.guild.publicRole.rawPermissions
    for(role in member.roles) {
        total = total or role.rawPermissions
    }
    return total
}

internal fun explicitPermissionsOf(channel: GuildChannel, member: Member): Long {
    check(channel.guild == member.guild) { permissionsWithUncommonGuild("channel", "member") }

    val permissions = explicitPermissionsOf(member)
    val (allow, deny) = processOverrides(channel, member)

    return apply(permissions, allow, deny)
}

internal fun explicitPermissionsOf(channel: GuildChannel, role: Role): Long {
    check(channel.guild == role.guild) { permissionsWithUncommonGuild("channel", "role") }

    val publicRole = role.guild.publicRole

    var permissions = role.rawPermissions or publicRole.rawPermissions
    var override = channel.permissionOverrideFor(publicRole)
    if(override != null)
        permissions = apply(permissions, override.rawAllowed, override.rawDenied)
    if(role == publicRole)
        return permissions

    override = channel.permissionOverrideFor(role)

    if(override == null) return permissions

    return apply(permissions, override.rawAllowed, override.rawDenied)
}

private fun applies(from: Long, perms: Long): Boolean = (from and perms) == perms

private fun apply(permissions: Long, allow: Long, deny: Long): Long {
    // deny all denied permissions, then apply allowed permissions
    // base < denied < allowed
    return permissions and deny.inv() or allow
}

private fun processOverrides(channel: GuildChannel, member: Member): Pair<Long, Long> {
    var allowedRaw = 0L
    var deniedRaw = 0L

    var override = channel.permissionOverrideFor(channel.guild.publicRole)
    if(override != null) {
        allowedRaw = override.rawAllowed
        deniedRaw = override.rawDenied
    }

    var allowedRoleRaw = 0L
    var deniedRoleRaw = 0L
    for(role in member.roles) {
        override = channel.permissionOverrideFor(role)
        if(override != null) {
            allowedRoleRaw = allowedRoleRaw or override.rawAllowed
            deniedRoleRaw = deniedRoleRaw or override.rawDenied
        }
    }

    allowedRaw = (allowedRaw and deniedRoleRaw.inv()) or allowedRoleRaw
    deniedRaw = (deniedRaw and allowedRoleRaw.inv()) or deniedRoleRaw

    override = channel.permissionOverrideFor(member)
    if(override != null) {
        val allowedMemberRaw = override.rawAllowed
        val deniedMemberRaw = override.rawDenied
        allowedRaw = (allowedRaw and deniedMemberRaw.inv()) or allowedMemberRaw
        deniedRaw = (deniedRaw and allowedMemberRaw.inv()) or deniedMemberRaw
    }

    return allowedRaw to deniedRaw
}

private fun interactionWithUncommonGuild(interacting: String, target: String = interacting): String {
    return "Interacting $interacting and target $target must be from the same guild!"
}

private fun permissionsWithUncommonGuild(env: String, of: String): String {
    return "Guild of $env and guild of $of must be the same guild!"
}
