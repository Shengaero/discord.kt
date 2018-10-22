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
import me.kgustave.dkt.internal.data.RawGuild
import me.kgustave.dkt.internal.data.RawUnavailableGuild
import me.kgustave.dkt.internal.data.events.RawGuildMembersChunkEvent
import me.kgustave.dkt.internal.data.events.RawReadyEvent
import me.kgustave.dkt.internal.data.events.RawResumeEvent
import me.kgustave.dkt.internal.data.serializers.IntPairArraySerializer
import me.kgustave.dkt.internal.impl.PresenceImpl
import me.kgustave.dkt.internal.websocket.EventType.*
import me.kgustave.dkt.util.IntPair
import me.kgustave.dkt.util.JsonParser
import me.kgustave.dkt.util.stringify

@Serializable
internal data class Payload(
    val op: Int,
    @Optional val d: Any? = null,
    @Optional val s: Long? = null,
    @Optional val t: EventType? = null,

    // This is used for debugging unknown events
    @Optional @Transient val debugT: String? = null
) {
    @Serializable
    data class Hello(
        @SerialName("heartbeat_interval") val heartbeatInterval: Long,
        @SerialName("_trace") val trace: Set<String>
    )

    @Serializable
    data class Identify(
        val token: String,
        val properties: Properties,
        val presence: PresenceImpl,
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

        @Serializer(forClass = Identify::class)
        companion object {
            override fun deserialize(input: Decoder): Identify {
                throw UnsupportedOperationException("Deserialization of Identify is not supported!")
            }

            override fun serialize(output: Encoder, obj: Identify) {
                @Suppress("NAME_SHADOWING")
                val output = output.beginStructure(descriptor)
                val (token, properties, presence, compress, largeThreshold, shard) = obj
                output.encodeStringElement(descriptor, descriptor.getElementIndex("token"), token)
                output.encodeSerializableElement(descriptor, descriptor.getElementIndex("properties"),
                    Properties.serializer(), properties)
                output.encodeSerializableElement(descriptor, descriptor.getElementIndex("presence"),
                    PresenceImpl.serializer(), presence)
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

    @Serializable
    data class GuildMemberRequest(
        @SerialName("guild_id")
        val guildId: List<Long>,
        val query: String,
        val limit: Int
    ) {
        @Serializer(forClass = GuildMemberRequest::class)
        companion object {
            override fun serialize(output: Encoder, obj: GuildMemberRequest) {
                check(output is JSON.JsonOutput)
                val json = json {
                    if(obj.guildId.size == 1) {
                        "guild_id" to obj.guildId[0].toString()
                    } else {
                        "guild_id" to JsonArray(obj.guildId.map { JsonPrimitive(it.toString()) })
                    }
                    "query" to obj.query
                    "limit" to obj.limit
                }
                output.writeTree(json)
            }
        }
    }

    @Serializer(forClass = Payload::class)
    companion object {
        override fun deserialize(input: Decoder): Payload {
            check(input is JSON.JsonInput) { "Decoder must be CompositeDecoder!" }
            val json = input.readAsTree().jsonObject
            val op = json["op"].int
            val d: Any? = when(op) {
                OP.Event -> {
                    val s = json["s"].long
                    val t = EventType.of(json["t"].content.replace(' ', '_').toUpperCase())
                    val d = deserializeEventData(t, json["d"])
                    return Payload(op, d, s, t, if(t == UNKNOWN) json["t"].content else null)
                }

                OP.Hello -> JsonParser.parse<Hello>("${json["d"]}")

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
            val op = obj.op
            output.encodeIntElement(descriptor, opIndex, op)
            when(op) {
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

                OP.StatusUpdate -> {
                    val presence = obj.d
                    require(presence is PresenceImpl) { "Expected PresenceImpl data!" }
                    output.encodeSerializableElement(descriptor, dIndex, PresenceImpl.serializer(), presence)
                }

                OP.RequestGuildMembers -> {
                    val guildMemberRequest = obj.d
                    require(guildMemberRequest is GuildMemberRequest) { "Expected GuildMemberRequest data!" }
                    output.encodeSerializableElement(descriptor, dIndex, GuildMemberRequest.serializer(), guildMemberRequest)
                }

                else -> throw UnsupportedOperationException("Serialization OP $op is not supported!")
            }

            output.endStructure(descriptor)
        }

        @JvmStatic private fun deserializeEventData(type: EventType, data: JsonElement): Any? {
            return when(type) {
                READY -> JsonParser.parse<RawReadyEvent>(data.stringify())
                RESUMED -> JsonParser.parse<RawResumeEvent>(data.stringify())
                GUILD_CREATE -> if(data.jsonObject.getOrNull("unavailable")?.booleanOrNull == true) {
                    JsonParser.parse<RawUnavailableGuild>(data.stringify())
                } else {
                    JsonParser.parse<RawGuild>(data.stringify())
                }
                GUILD_MEMBERS_CHUNK -> JsonParser.parse<RawGuildMembersChunkEvent>(data.stringify())
                else -> data
            }
        }
    }
}
