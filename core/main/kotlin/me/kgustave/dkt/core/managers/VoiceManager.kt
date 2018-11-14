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
@file:Suppress("unused")
package me.kgustave.dkt.core.managers

import me.kgustave.dkt.core.DiscordBot
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.core.entities.Guild
import me.kgustave.dkt.core.entities.VoiceChannel
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.internal.entities.GuildImpl

@ExperimentalVoiceAPI
interface VoiceManager {
    val bot: DiscordBot
    val guild: Guild
    val connectedChannel: VoiceChannel
    var sender: VoiceManager.Sender?

    fun connectTo(channel: VoiceChannel)

    fun disconnect()

    @ExperimentalVoiceAPI
    interface Sender {
        val isOpus: Boolean

        fun provide20MsAudio(): ByteArray?
        fun canProvide(): Boolean

        @DktInternalExperiment
        suspend fun awaitToProvide() {}
    }

    @ExperimentalVoiceAPI
    interface Provider {
        fun provide(guild: GuildImpl): VoiceManager
    }
}
