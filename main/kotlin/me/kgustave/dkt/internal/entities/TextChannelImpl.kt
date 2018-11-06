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

import me.kgustave.dkt.entities.TextChannel
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.promises.MessagePromise

@DktInternal
class TextChannelImpl(
    id: Long,
    guild: GuildImpl
): TextChannel, AbstractGuildChannelImpl(id, guild) {
    internal var parentId: Long? = null

    override val parent: CategoryImpl? get() = parentId?.let { guild.categoryCache[it] }
    override val position: Int get() = guild.textChannels.binarySearch(this)

    override var topic: String? = null
    override var nsfw: Boolean = false
    override var rateLimitPerUser: Int = 0
    override var lastMessageId: Long? = null

    override fun send(text: String): MessagePromise = MessagePromise(bot, this, text)

    override fun compareTo(other: TextChannel): Int {
        if(this === other) return 0
        if(this == other) return 0

        require(guild == other.guild) { "Cannot compare text channels from different guilds!" }

        if(rawPosition == other.rawPosition) return id.compareTo(other.id)
        return rawPosition.compareTo(other.rawPosition)
    }
}
