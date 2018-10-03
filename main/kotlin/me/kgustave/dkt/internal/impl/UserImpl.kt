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

import kotlinx.serialization.json.json
import me.kgustave.dkt.entities.PrivateChannel
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.internal.data.RawUser
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.internal.rest.emptyPromise
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.internal.rest.restPromise

internal open class UserImpl(override val bot: DiscordBotImpl, raw: RawUser): User {
    override val id: Long = raw.id
    override val isBot: Boolean = raw.bot
    override var name: String = raw.username
    override var discriminator: Int = raw.discriminator.toInt()
    override var avatarHash: String? = raw.avatar
    override val defaultAvatarHash: String get() = DefaultAvatars[discriminator % 5]
    override val avatarUrl: String get() {
        avatarHash?.let {
            if(it.startsWith("a_"))
                return "$AvatarBaseUrl/$id/$it.gif"
            return "$AvatarBaseUrl/$id/$it.png"
        }
        return "$DefaultAvatarBaseUrl/$defaultAvatarHash.png"
    }

    private var privateChannel: PrivateChannel? = null

    internal fun patch(raw: RawUser) {
        this.name = raw.username
        this.discriminator = raw.discriminator.toInt()
        this.avatarHash = raw.avatar
    }

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        privateChannel?.let { return bot.emptyPromise(it) }

        val body = json { "recipient_id" to id }
        return bot.restPromise<PrivateChannel>(Route.CreateDM, body = body) {
            TODO()
        }
    }

    companion object {
        const val AvatarBaseUrl = "${Requester.CDNBaseUrl}/avatars"
        const val DefaultAvatarBaseUrl = "${Requester.CDNBaseUrl}/embed/avatars"
        val DefaultAvatars = arrayOf(
            "6debd47ed13483642cf09e832ed0bc1b", // blurple
            "322c936a8c8be1b803cd94861bdfa868", // gray
            "dd4dbc0016779df1378e7812eabaa04d", // green
            "0e291f67c9274a1abdddeb3fd919cbaa", // orange
            "1cbd08c76f8af6dddce02c5138971129"  // red
        )
    }
}
