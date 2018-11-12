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
package me.kgustave.dkt.core.internal.websocket

internal enum class CloseStatus(val code: Int, val mayReconnect: Boolean = true) {
    GRACEFUL_CLOSE(1000),
    CLOUD_FLARE_LOAD(1001),
    INTERNAL_SERVER_ERROR(1006),
    UNKNOWN_ERROR(4000),
    UNKNOWN_OPCODE(4001, false),
    DECODE_ERROR(4002, false),
    NOT_AUTHENTICATED(4003),
    AUTHENTICATION_FAILED(4004, false),
    ALREADY_AUTHENTICATED(4005, false),
    INVALID_SEQ(4007),
    RATE_LIMITED(4008, false),
    SESSION_TIMEOUT(4009),
    INVALID_SHARD(4010, false),
    SHARDING_REQUIRED(4011, false);

    companion object {
        fun of(code: Int) = CloseStatus.values().firstOrNull { it.code == code }
    }
}
