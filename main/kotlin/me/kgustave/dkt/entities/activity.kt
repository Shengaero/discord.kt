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
@file:Suppress("FoldInitializerAndIfToElvis", "RemoveEmptyPrimaryConstructor", "unused")
package me.kgustave.dkt.entities

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import me.kgustave.dkt.util.IntPair
import java.time.OffsetDateTime

fun playing(game: String): Activity = BasicActivity(game, Activity.Type.GAME)
fun streaming(title: String, url: String): Activity = BasicActivity(title, Activity.Type.STREAMING, url)
fun listeningTo(name: String): Activity = BasicActivity(name, Activity.Type.LISTENING)
fun watching(name: String): Activity = BasicActivity(name, Activity.Type.WATCHING)

sealed class Activity {
    abstract val name: String
    abstract val type: Activity.Type
    abstract val url: String?
    abstract val timestamps: Timestamps?

    data class Timestamps internal constructor(val begin: Long? = null, val end: Long? = null) {
        val beginTime: OffsetDateTime get() = TODO("Not Implemented Yet")
        val endTime: OffsetDateTime get() = TODO("Not Implemented Yet")
    }

    enum class Type {
        GAME, STREAMING, LISTENING, WATCHING, UNKNOWN;

        companion object {
            @JvmStatic fun of(type: Int): Type {
                return values().firstOrNull { it.ordinal == type } ?: UNKNOWN
            }
        }
    }
}

class BasicActivity(
    override val name: String,
    override val type: Activity.Type,
    override val url: String? = null,
    override val timestamps: Activity.Timestamps? = null
): Activity()

class RichPresenceActivity internal constructor(
    override val name: String,
    override val type: Activity.Type,
    override val url: String?,
    override val timestamps: Activity.Timestamps?,
    val applicationId: Long,
    val details: String? = null,
    val state: String? = null,
    val party: RichPresenceActivity.Party? = null
): Activity() {
    data class Party internal constructor(
        val id: String? = null,
        val size: IntPair? = null
    )
}

@Serializer(forClass = Activity::class)
internal object ActivitySerializer {
    override fun deserialize(input: Decoder): Activity {
        check(input is JSON.JsonInput) { "Decoder was not a JsonInput!" }
        val json = input.readAsTree().jsonObject

        val name = json["name"].content
        val type = Activity.Type.of(json["type"].int)
        val url = json["url"].contentOrNull
        val timestamps = json.getObjectOrNull("timestamps")?.let {
            Activity.Timestamps(it["begin"].longOrNull, it["end"].longOrNull)
        }

        val applicationId = json["application_id"].contentOrNull?.toLongOrNull()

        if(applicationId == null) return BasicActivity(name, type, url, timestamps)

        return RichPresenceActivity(name, type, url, timestamps, applicationId)
    }

    override fun serialize(output: Encoder, obj: Activity) {
        check(output is JSON.JsonOutput) { "Encoder must be JsonOutput!" }
        check(obj !is RichPresenceActivity) { "Cannot serialize RichPresenceActivity!" }
        val json = json {
            "name" to obj.name
            "type" to obj.type.ordinal
            obj.url?.let { url -> "url" to url }
        }
        output.writeTree(json)
    }
}
