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

import me.kgustave.dkt.core.entities.Category
import me.kgustave.dkt.core.entities.TextChannel
import me.kgustave.dkt.core.entities.VoiceChannel
import me.kgustave.dkt.core.internal.DktInternal

@DktInternal
class CategoryImpl
internal constructor(id: Long, guild: GuildImpl): Category, AbstractGuildChannelImpl(id, guild) {
    override val parent: CategoryImpl? get() = null
    override val position: Int get() = guild.categories.binarySearch(this)

    override val textChannels: List<TextChannel> get() {
        return guild.textChannels.filter { it.parent == this }
    }

    override val voiceChannels: List<VoiceChannel> get() {
        return guild.voiceChannels.filter { it.parent == this }
    }

    override fun compareTo(other: Category): Int {
        if(this === other) return 0
        if(this == other) return 0

        require(guild == other.guild) { "Cannot compare categories from different guilds!" }

        if(rawPosition == other.rawPosition) return id.compareTo(other.id)
        return rawPosition.compareTo(other.rawPosition)
    }
}
