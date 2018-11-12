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

interface Member: Mentionable, PermissionHolder {
    val bot: DiscordBot
    val guild: Guild
    val user: User
    val nickname: String?
    val roles: List<Role>
    val voiceState: GuildVoiceState
    val activity: Activity?
    val status: OnlineStatus

    val name: String get() = nickname ?: user.name
    val isOwner: Boolean get() = this == guild.owner
    override val mention: String get() = if(nickname != null) "<@!${user.id}>" else user.mention
}
