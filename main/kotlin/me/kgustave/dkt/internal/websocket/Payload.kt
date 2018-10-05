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
package me.kgustave.dkt.internal.websocket

import kotlinx.serialization.*
import kotlinx.serialization.internal.makeNullable
import kotlinx.serialization.json.*
import me.kgustave.dkt.entities.OnlineStatus
import me.kgustave.dkt.internal.data.events.RawReadyEvent
import me.kgustave.dkt.internal.data.serializers.IntPairArraySerializer
import me.kgustave.dkt.util.IntPair
import me.kgustave.dkt.util.stringify

@Serializable
internal data class Payload(
    val op: Int,
    @Optional val d: Any? = null,
    @Optional val s: Long? = null,
    @Optional val t: String? = null
) {
    @Serializable
    data class Hello(
        @SerialName("heartbeat_interval") val heartbeatInterval: Long,
        @SerialName("_trace") val trace: List<String>
    )

    @Serializable
    data class Identify(
        val token: String,
        val properties: Properties,
        val presence: Presence,
        @Optional val compress: Boolean = false,
        @Optional @SerialName("large_threshold") val largeThreshold: Int = 50,
        @Optional val shard: IntPair? = null
    ) {
        @Serializable
        data class Properties(
            @SerialName("\$os") val os: String,
            @SerialName("\$browser") val browser: String,
            @SerialName("\$device") val device: String
        )

        @Serializable
        data class Presence(
            val status: OnlineStatus,
            val afk: Boolean,
            @Optional val game: Activity? = null
        ) {
            @Serializable
            data class Activity(val name: String, val type: Int)
        }

        @Serializer(forClass = Identify::class)
        companion object {
            override fun serialize(output: Encoder, obj: Identify) {
                @Suppress("NAME_SHADOWING")
                val output = output.beginStructure(descriptor)
                val (token, properties, presence, compress, largeThreshold, shard) = obj
                output.encodeStringElement(descriptor, descriptor.getElementIndex("token"), token)
                output.encodeSerializableElement(descriptor, descriptor.getElementIndex("properties"),
                    Properties.serializer(), properties)
                output.encodeSerializableElement(descriptor, descriptor.getElementIndex("presence"),
                    Presence.serializer(), presence)
                output.encodeBooleanElement(descriptor, descriptor.getElementIndex("compress"), compress)
                output.encodeIntElement(descriptor, descriptor.getElementIndex("large_threshold"), largeThreshold)
                if(shard != null) {
                    output.encodeSerializableElement(descriptor, descriptor.getElementIndex("shard"),
                        IntPairArraySerializer, shard)
                }
                output.endStructure(descriptor)
            }
        }
    }

    @Serializable
    data class Resume(
        val token: String,
        @SerialName("session_id") val sessionId: String,
        val seq: Long
    )

    @Serializer(forClass = Payload::class)
    internal companion object {
        override fun deserialize(input: Decoder): Payload {
            check(input is JSON.JsonInput) { "Decoder must be CompositeDecoder!" }
            val json = input.readAsTree().jsonObject
            val op = json["op"].int
            val d: Any? = when(op) {
                OP.Event -> {
                    val s = json["s"].long
                    val t = json["t"].content
                    val d = deserializeEventData(t, json["d"])
                    return Payload(op, d, s, t)
                }

                OP.Hello -> JSON.nonstrict.parse<Hello>("${json["d"]}")

                OP.Heartbeat, OP.HeartbeatACK -> null

                OP.InvalidSession -> json["d"].boolean

                // In the event of an unknown OP code, our WebSocket
                //should handle everything. There is no point in throwing
                //an exception here because it really just screws up
                //the supply line if Discord ever were to start sending a
                //new OP out of the blue.
                else -> null
            }

            return Payload(op, d)
        }

        override fun serialize(output: Encoder, obj: Payload) {
            @Suppress("NAME_SHADOWING")
            val output = output.beginStructure(descriptor)
            val opIndex = descriptor.getElementIndex("op")
            val dIndex = descriptor.getElementIndex("d")
            output.encodeIntElement(descriptor, opIndex, obj.op)
            when(obj.op) {
                OP.Heartbeat -> {
                    val d = obj.d
                    if(d is Long) {
                        val s = makeNullable(Long.serializer())
                        output.encodeNullableSerializableElement(descriptor, dIndex, s, d)
                    } else {
                        val s = makeNullable(Int.serializer())
                        output.encodeNullableSerializableElement(descriptor, dIndex, s, d as? Int)
                    }
                }

                OP.Identify -> {
                    val identify = obj.d
                    require(identify is Identify) { "Expected Identify data!" }
                    output.encodeSerializableElement(descriptor, dIndex, Identify.serializer(), identify)
                }

                OP.Resume -> {
                    val resume = obj.d
                    require(resume is Resume) { "Expected Resume data!" }
                    output.encodeSerializableElement(descriptor, dIndex, Resume.serializer(), resume)
                }

                else -> throw UnsupportedOperationException("Serialization OP ${obj.op} is not supported!")
            }

            output.endStructure(descriptor)
        }

        @JvmStatic private fun deserializeEventData(type: String, data: JsonElement): Any? {
            return when(type) {
                "READY" -> JSON.nonstrict.parse<RawReadyEvent>(data.stringify())
                else -> data
            }
        }
    }
}
