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
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.internal.entities.GuildImpl
import me.kgustave.dkt.core.internal.entities.VoiceChannelImpl
import me.kgustave.dkt.core.managers.VoiceManager
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.util.delegates.cleaningRef
import me.kgustave.dkt.util.delegates.weak
import java.util.concurrent.locks.ReentrantLock

@DktInternal
@ExperimentalVoiceAPI
@DktInternalExperiment
internal class VoiceManagerImpl(guild: GuildImpl): VoiceManager {
    override var sender: VoiceManager.Sender? = null

    internal var connectingChannel: VoiceChannelImpl? by cleaningRef(threadSafe = true)

    private var _connectedChannel: VoiceChannel? = null

    override val guild by weak(guild)

    override val bot get() = guild.bot

    // FIXME This should be nullable
    override val connectedChannel: VoiceChannel get() = _connectedChannel ?: error("No connected channel")

    internal val connectionLock = ReentrantLock()

    override fun connectTo(channel: VoiceChannel) {
        require(guild == channel.guild) { "VoiceChannel was not from the same Guild!" }
        require(!guild.unavailable) { "Guild is not available!" }

        val self = guild.self

    }

    override fun disconnect() {
    }
}
