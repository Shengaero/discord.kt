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
package me.kgustave.dkt.internal.impl

import kotlinx.serialization.*
import me.kgustave.dkt.entities.Activity
import me.kgustave.dkt.internal.data.serializers.ActivitySerializer
import me.kgustave.dkt.entities.OnlineStatus
import me.kgustave.dkt.entities.Presence
import me.kgustave.dkt.util.currentTimeMs

@Serializable
internal data class PresenceImpl(
    override val status: OnlineStatus,
    override val afk: Boolean,
    @Optional @SerialName("game") override val activity: Activity? = null,
    @Optional @SerialName("since") val afkSince: Long? = null
): Presence {
    constructor(builder: Presence.Builder): this(builder.status, builder.afk, builder.activity)

    @Serializer(forClass = PresenceImpl::class)
    companion object {
        override fun serialize(output: Encoder, obj: PresenceImpl) {
            val out = output.beginStructure(descriptor)
            out.encodeStringElement(descriptor, descriptor.getElementIndex("status"), obj.status.statusName.toLowerCase())
            out.encodeBooleanElement(descriptor, descriptor.getElementIndex("afk"), obj.afk)
            obj.activity?.let { activity ->
                out.encodeSerializableElement(descriptor, descriptor.getElementIndex("game"), ActivitySerializer, activity)
            }
            out.encodeLongElement(descriptor, descriptor.getElementIndex("since"), obj.afkSince ?: currentTimeMs)
            out.endStructure(descriptor)
        }
    }
}
