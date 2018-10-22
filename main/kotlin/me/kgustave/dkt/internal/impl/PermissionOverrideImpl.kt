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

import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.GuildChannel
import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.PermissionOverride
import me.kgustave.dkt.entities.Role
import me.kgustave.dkt.promises.RestPromise

internal class PermissionOverrideImpl: PermissionOverride {
    override val rawAllowed: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val rawInherited: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val rawDenied: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val allowed: List<Permission>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val inherited: List<Permission>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val denied: List<Permission>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val bot: DiscordBot
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val channel: GuildChannel
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val member: Member?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val role: Role?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun delete(): RestPromise<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
