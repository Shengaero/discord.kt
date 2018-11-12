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
package me.kgustave.dkt.voice

import me.kgustave.dkt.core.entities.VoiceChannel
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.core.internal.entities.GuildImpl
import me.kgustave.dkt.core.internal.entities.VoiceChannelImpl
import me.kgustave.dkt.core.managers.VoiceManager
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.util.delegates.cleaningRef
import me.kgustave.dkt.util.delegates.weak

@DktInternal
@ExperimentalVoiceAPI
internal class VoiceManagerImpl(guild: GuildImpl): VoiceManager {
    override val guild by weak(guild)
    override val bot get() = guild.bot
    override val connectedChannel: VoiceChannel
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override var sender: VoiceManager.Sender?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    internal var connectingChannel: VoiceChannelImpl? by cleaningRef(threadSafe = true)

    override fun connectTo(channel: VoiceChannel) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun disconnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
