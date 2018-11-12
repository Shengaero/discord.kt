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
import me.kgustave.dkt.internal.data.RawChannel
import me.kgustave.dkt.internal.data.RawGuild
import me.kgustave.dkt.internal.data.RawMember
import me.kgustave.dkt.internal.data.RawUnavailableGuild
import me.kgustave.dkt.internal.data.events.*
import me.kgustave.dkt.internal.data.serializers.IntPairArraySerializer
import me.kgustave.dkt.internal.entities.PresenceImpl
import me.kgustave.dkt.internal.util.createStructure
import me.kgustave.dkt.internal.websocket.EventType.*
import me.kgustave.dkt.internal.util.JsonParser
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
        @Optional val shard: Pair<Int, Int>? = null
    ) {
        @Serializable
        data class Properties(
            @SerialName("\$os") val os: String,
            @SerialName("\$browser") val browser: String,
            @SerialName("\$device") val device: String
        )

        @Serializer(forClass = Identify::class)
        companion object {
            override fun serialize(output: Encoder, obj: Identify) {
                output.createStructure(descriptor) {
                    val (token, properties, presence, compress, largeThreshold, shard) = obj
                    encodeStringElement(descriptor, descriptor.getElementIndex("token"), token)
                    encodeSerializableElement(descriptor, descriptor.getElementIndex("properties"),
                        Properties.serializer(), properties)
                    encodeSerializableElement(descriptor, descriptor.getElementIndex("presence"),
                        PresenceImpl.serializer(), presence)
                    encodeBooleanElement(descriptor, descriptor.getElementIndex("compress"), compress)
                    encodeIntElement(descriptor, descriptor.getElementIndex("large_threshold"), largeThreshold)
                    if(shard != null) {
                        encodeSerializableElement(descriptor, descriptor.getElementIndex("shard"),
                            IntPairArraySerializer, shard)
                    }
                }
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
                output.createStructure(descriptor) {
                    val guildIdIndex = descriptor.getElementIndex("guild_id")
                    if(obj.guildId.size == 1) {
                        encodeStringElement(descriptor, guildIdIndex, obj.guildId[0].toString())
                    } else {
                        encodeSerializableElement(descriptor, guildIdIndex,
                            String.serializer().list, obj.guildId.map(Long::toString))
                    }
                    encodeStringElement(descriptor, descriptor.getElementIndex("query"), obj.query)
                    encodeIntElement(descriptor, descriptor.getElementIndex("limit"), obj.limit)
                }
            }
        }
    }

    @Serializer(forClass = Payload::class)
    companion object {
        override fun deserialize(input: Decoder): Payload {
            require(input is JSON.JsonInput) { "Decoder must be CompositeDecoder!" }
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
            output.createStructure(descriptor) {
                encodeIntElement(descriptor, descriptor.getElementIndex("op"), obj.op)
                val dIndex = descriptor.getElementIndex("d")
                when(obj.op) {
                    OP.Heartbeat -> {
                        val d = obj.d
                        if(d is Long) {
                            val s = makeNullable(Long.serializer())
                            encodeNullableSerializableElement(descriptor, dIndex, s, d)
                        } else {
                            val s = makeNullable(Int.serializer())
                            encodeNullableSerializableElement(descriptor, dIndex, s, d as? Int)
                        }
                    }

                    OP.Identify -> {
                        val identify = obj.d
                        require(identify is Identify) { "Expected Identify data!" }
                        encodeSerializableElement(descriptor, dIndex, Identify.serializer(), identify)
                    }

                    OP.Resume -> {
                        val resume = obj.d
                        require(resume is Resume) { "Expected Resume data!" }
                        encodeSerializableElement(descriptor, dIndex, Resume.serializer(), resume)
                    }

                    OP.StatusUpdate -> {
                        val presence = obj.d
                        require(presence is PresenceImpl) { "Expected PresenceImpl data!" }
                        encodeSerializableElement(descriptor, dIndex, PresenceImpl.serializer(), presence)
                    }

                    OP.RequestGuildMembers -> {
                        val guildMemberRequest = obj.d
                        require(guildMemberRequest is GuildMemberRequest) { "Expected GuildMemberRequest data!" }
                        encodeSerializableElement(descriptor, dIndex, GuildMemberRequest.serializer(), guildMemberRequest)
                    }

                    else -> throw UnsupportedOperationException("Serialization OP ${obj.op} is not supported!")
                }
            }
        }

        @JvmStatic private fun deserializeEventData(type: EventType, data: JsonElement): Any? {
            return when(type) {
                READY -> JsonParser.parse<RawReadyEvent>(data.stringify())
                RESUMED -> JsonParser.parse<RawResumeEvent>(data.stringify())

                CHANNEL_CREATE,
                CHANNEL_UPDATE,
                CHANNEL_DELETE -> JsonParser.parse<RawChannel>(data.stringify())

                GUILD_CREATE -> {
                    if(data.jsonObject.getOrNull("unavailable")?.booleanOrNull == true) {
                        JsonParser.parse<RawUnavailableGuild>(data.stringify())
                    } else {
                        JsonParser.parse<RawGuild>(data.stringify())
                    }
                }

                GUILD_DELETE -> JsonParser.parse<RawUnavailableGuild>(data.stringify())

                GUILD_BAN_ADD,
                GUILD_BAN_REMOVE -> JsonParser.parse<RawGuildBanEvent>(data.stringify())

                GUILD_MEMBER_ADD -> JsonParser.parse<RawMember>(data.stringify())
                GUILD_MEMBER_UPDATE -> JsonParser.parse<RawGuildMemberUpdateEvent>(data.stringify())
                GUILD_MEMBER_REMOVE -> JsonParser.parse<RawGuildMemberRemoveEvent>(data.stringify())

                TYPING_START -> JsonParser.parse<RawTypingStartEvent>(data.stringify())

                GUILD_MEMBERS_CHUNK -> JsonParser.parse<RawGuildMembersChunkEvent>(data.stringify())

                MESSAGE_CREATE,
                UNKNOWN -> data
            }
        }
    }
}
