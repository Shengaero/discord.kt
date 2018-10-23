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

import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.VoiceChannel

internal class VoiceChannelImpl(
    id: Long,
    guild: GuildImpl
): VoiceChannel, AbstractGuildChannelImpl(id, guild) {
    internal var parentId: Long? = null
    internal val connectedMembers = hashMapOf<Long, Member>()

    override val parent: CategoryImpl? get() = parentId?.let { guild.categoryCache[it] }

    override var userLimit = 0
    override var bitrate = 0

    override val position: Int get() = guild.voiceChannels.binarySearch(this)

    override fun compareTo(other: VoiceChannel): Int {
        if(this === other) return 0
        if(this == other) return 0

        require(guild == other.guild) { "Cannot compare voice channels from different guilds!" }

        if(rawPosition == other.rawPosition) return id.compareTo(other.id)
        return rawPosition.compareTo(other.rawPosition)
    }
}
