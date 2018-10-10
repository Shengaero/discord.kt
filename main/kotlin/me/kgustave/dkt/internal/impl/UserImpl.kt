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
import me.kgustave.dkt.internal.rest.emptyPromise
import me.kgustave.dkt.internal.rest.restPromise
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route

internal open class UserImpl(override val bot: DiscordBotImpl, raw: RawUser): User {
    override val id: Long = raw.id
    override val isBot: Boolean = raw.bot
    override var name: String = raw.username
    override var discriminator: Int = raw.discriminator.toInt()
    override var avatarHash: String? = raw.avatar
    override val defaultAvatarHash: String get() = User.DefaultAvatarHashes[discriminator % 5]
    override val avatarUrl: String get() {
        avatarHash?.let { avatarHash ->
            val suffix = if(avatarHash.startsWith("a_")) "gif" else "png"
            return "${User.AvatarBaseUrl}/$id/$avatarHash.$suffix"
        }
        return "${User.DefaultAvatarBaseUrl}/$defaultAvatarHash.png"
    }

    private var privateChannel: PrivateChannel? = null

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        privateChannel?.let { return bot.emptyPromise(it) }
        val body = json { "recipient_id" to id }
        return bot.restPromise(Route.CreateDM, body = body) {
            TODO()
        }
    }

    /**
     * Patches the [UserImpl] with the data contained by the
     * provided [raw] user instance.
     */
    internal fun patch(raw: RawUser) {
        require(id == raw.id) { "User ID mismatch! Expected $id, Actual: ${raw.id}" }

        this.name = raw.username
        this.discriminator = raw.discriminator.toInt()
        this.avatarHash = raw.avatar
    }
}
