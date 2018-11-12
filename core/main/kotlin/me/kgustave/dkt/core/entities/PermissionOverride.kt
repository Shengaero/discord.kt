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
package me.kgustave.dkt.core.entities

import me.kgustave.dkt.core.DiscordBot
import me.kgustave.dkt.core.Permission
import me.kgustave.dkt.core.promises.RestPromise

interface PermissionOverride {
    val rawAllowed: Long
    val rawInherited: Long
    val rawDenied: Long

    val allowed: List<Permission>
    val inherited: List<Permission>
    val denied: List<Permission>

    val bot: DiscordBot
    val channel: GuildChannel
    val member: Member?
    val role: Role?
    val guild: Guild get() = channel.guild

    fun delete(): RestPromise<Unit>
}
