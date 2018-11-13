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

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import me.kgustave.dkt.core.internal.DktInternalExperiment
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI

@Serializable
@ExperimentalVoiceAPI
@DktInternalExperiment
data class VoicePayload(val op: Int, @Optional val d: Any? = null) {
    @Serializable
    data class Identify(
        @SerialName("server_id")
        val serverId: Long,
        @SerialName("user_id")
        val userId: Long,
        @SerialName("session_id")
        val sessionId: String,
        val token: String
    ) {
        @Serializer(forClass = Identify::class)
        companion object {
            override fun serialize(output: Encoder, obj: Identify) {
                val out = output.beginStructure(descriptor)

                out.encodeStringElement(descriptor, descriptor.getElementIndex("server_id"), obj.serverId.toString())
                out.encodeStringElement(descriptor, descriptor.getElementIndex("user_id"), obj.userId.toString())
                out.encodeStringElement(descriptor, descriptor.getElementIndex("session_id"), obj.sessionId)
                out.encodeStringElement(descriptor, descriptor.getElementIndex("token"), obj.token)

                out.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class Hello(@SerialName("heartbeat_interval") val heartbeatInterval: Long)

    @Serializable
    data class Ready(
        val ssrc: Int,
        val ip: String,
        val port: Int,
        val modes: List<String>,
        @SerialName("heartbeat_interval")
        val heartbeatInterval: Long
    )

    @Serializable
    data class Speaking(val speaking: Int, val ssrc: Int, val delay: Int)

    @Serializable
    data class SessionDescription(
        val mode: String,
        @SerialName("secret_key")
        val secretKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if(this === other) return true
            if(javaClass != other?.javaClass) return false

            other as SessionDescription

            if(mode != other.mode) return false
            if(!secretKey.contentEquals(other.secretKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mode.hashCode()
            result = 31 * result + secretKey.contentHashCode()
            return result
        }
    }

    @Serializer(forClass = VoicePayload::class)
    companion object {
        override fun deserialize(input: Decoder): VoicePayload {
            check(input is JSON.JsonInput)

            val json = input.readAsTree().jsonObject
            val op = json["op"].int

            val data: Any? = when(op) {
                VOP.Ready -> {
                    val d = json["d"].jsonObject
                    Ready(
                        ssrc = d["ssrc"].int,
                        ip = d["ip"].content,
                        port = d["port"].int,
                        modes = d["modes"].jsonArray.map { it.content },
                        heartbeatInterval = d["heartbeat_interval"].long
                    )
                }

                VOP.Speaking -> {
                    val d = json["d"]
                }

                VOP.SessionDescription -> {
                    val d = json["d"].jsonObject
                    val mode = d["mode"].content
                    val secretKey = d["secret_key"].jsonArray.map { it.int.toByte() }.toByteArray()
                    SessionDescription(mode, secretKey)
                }

                VOP.Hello -> {
                    val d = json["d"].jsonObject
                    Hello(d["heartbeat_interval"].long)
                }

                VOP.HeartbeatACK -> json["d"].long

                else -> json.getOrNull("d")
            }

            return VoicePayload(op, data)
        }

        override fun serialize(output: Encoder, obj: VoicePayload) {
            val struct = output.beginStructure(descriptor)

            struct.encodeIntElement(descriptor, descriptor.getElementIndex("op"), obj.op)

            val dIndex = descriptor.getElementIndex("d")

            when(obj.op) {
                VOP.Identify -> {
                    val d = obj.d as Identify
                    struct.encodeSerializableElement(descriptor, dIndex, Identify.serializer(), d)
                }

                VOP.Heartbeat -> {
                    struct.encodeLongElement(descriptor, dIndex, obj.d as Long)
                }

                VOP.Speaking -> {
                    val d = obj.d as Speaking
                    struct.encodeSerializableElement(descriptor, dIndex, Speaking.serializer(), d)
                }
            }

            struct.endStructure(descriptor)
        }
    }
}
