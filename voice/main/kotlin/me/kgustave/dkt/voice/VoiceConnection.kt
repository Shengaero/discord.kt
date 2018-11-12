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
@file:Suppress("PropertyName")
package me.kgustave.dkt.voice

import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.delegates.weak
import opus.OpusEncoder
import java.net.DatagramSocket

@DktInternal
@ExperimentalVoiceAPI
internal class VoiceConnection(
    private val manager: VoiceManagerImpl,
    bot: DiscordBotImpl
) {
    @Volatile private var _upd: DatagramSocket? = null
    private var _encoder: OpusEncoder? = null
    private var _channel by weak(manager.connectingChannel)

    val bot by weak(bot)
    val channel get() = _channel ?: error("No channel being connected to!")
    val udp: DatagramSocket get() = _upd ?: error("UDP is not connected!")
    val encoder: OpusEncoder get() = _encoder ?: error("Encoder not available!")

    companion object {
        val Log = createLogger(VoiceConnection::class)
    }
}
