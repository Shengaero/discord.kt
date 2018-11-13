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
package me.kgustave.dkt.core.voice

import me.kgustave.dkt.core.entities.VoiceChannel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer

@ExperimentalVoiceAPI
interface PacketChannel {
    val channel: VoiceChannel
    val udp: DatagramSocket
    val address: InetSocketAddress

    suspend fun nextPacketRaw(nowTalking: Boolean): ByteBuffer?

    suspend fun nextPacket(nowTalking: Boolean): DatagramPacket?

    fun onConnectionLost()
}
