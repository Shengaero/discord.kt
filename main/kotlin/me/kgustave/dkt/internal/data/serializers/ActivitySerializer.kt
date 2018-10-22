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
@file:Suppress("FoldInitializerAndIfToElvis")
package me.kgustave.dkt.internal.data.serializers

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.*
import me.kgustave.dkt.entities.Activity
import me.kgustave.dkt.entities.BasicActivity
import me.kgustave.dkt.entities.RichPresenceActivity

@Serializer(forClass = Activity::class)
internal object ActivitySerializer {
    override fun deserialize(input: Decoder): Activity {
        check(input is JSON.JsonInput) { "Decoder was not a JsonInput!" }
        val json = input.readAsTree().jsonObject

        val name = json["name"].content
        val type = Activity.Type.of(json["type"].int)
        val url = json.getOrNull("url")?.contentOrNull
        val timestamps = json.getObjectOrNull("timestamps")?.let {
            Activity.Timestamps(it.getOrNull("begin")?.longOrNull, it.getOrNull("end")?.longOrNull)
        }

        val applicationId = json.getOrNull("application_id")?.longOrNull

        if(applicationId == null) return BasicActivity(name, type, url, timestamps)

        // FIXME return RichPresenceActivity(name, type, url, timestamps, applicationId)
        return BasicActivity(name, type, url, timestamps)
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
