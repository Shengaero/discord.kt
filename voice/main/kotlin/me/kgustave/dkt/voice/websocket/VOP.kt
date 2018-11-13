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
package me.kgustave.dkt.voice.websocket

import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI

// Voice Op Codes
@ExperimentalVoiceAPI
@DktInternalExperiment
object VOP {
    const val Identify = 0 // client
    const val SelectProtocol = 1 // client
    const val Ready = 2 // server
    const val Heartbeat = 3 // client
    const val SessionDescription = 4 // server
    const val Speaking = 5 // client & server
    const val HeartbeatACK = 6 // server
    const val Resume = 7 // client
    const val Hello = 8 // server
    const val Resumed = 9 // server
    const val ClientDisconnect = 13 // server
}
