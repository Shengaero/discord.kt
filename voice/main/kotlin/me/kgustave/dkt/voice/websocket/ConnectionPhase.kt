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

@ExperimentalVoiceAPI
@DktInternalExperiment
internal enum class ConnectionPhase(val shouldReconnect: Boolean = true) {
    NOT_CONNECTED(false),
    SHUTTING_DOWN(false),
    CONNECTING_AWAITING_ENDPOINT,
    CONNECTING_AWAITING_WEBSOCKET_CONNECT,
    CONNECTING_AWAITING_AUTHENTICATION,
    CONNECTING_ATTEMPTING_UDP_DISCOVERY,
    CONNECTING_AWAITING_READY,
    CONNECTED,
    DISCONNECTED_LOST_PERMISSION(false),
    DISCONNECTED_CHANNEL_DELETED(false),
    DISCONNECTED_REMOVED_FROM_GUILD(false),
    DISCONNECTED_REMOVED_DURING_RECONNECT(false),
    DISCONNECTED_AUTHENTICATION_FAILURE,
    AUDIO_REGION_CHANGE,

    // the ones below are disabled when shouldReconnect is false
    ERROR_LOST_CONNECTION,
    ERROR_CANNOT_RESUME,
    ERROR_WEBSOCKET_UNABLE_TO_CONNECT,
    ERROR_UNSUPPORTED_ENCRYPTION_MODES,
    ERROR_UDP_UNABLE_TO_CONNECT,
    ERROR_CONNECTION_TIMEOUT;
}
