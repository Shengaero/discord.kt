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

import me.kgustave.dkt.entities.Category
import me.kgustave.dkt.entities.TextChannel
import me.kgustave.dkt.entities.VoiceChannel

internal class CategoryImpl(
    id: Long,
    guild: GuildImpl
): Category, AbstractGuildChannelImpl(id, guild) {
    override val parent: CategoryImpl? get() = null
    override val textChannels: List<TextChannel>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val voiceChannels: List<VoiceChannel>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}
