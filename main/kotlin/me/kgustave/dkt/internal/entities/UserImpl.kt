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
package me.kgustave.dkt.internal.entities

import io.ktor.client.call.receive
import kotlinx.serialization.json.json
import me.kgustave.dkt.entities.PrivateChannel
import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.internal.data.RawChannel
import me.kgustave.dkt.internal.data.RawUserData
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.promises.emptyPromise
import me.kgustave.dkt.promises.restPromise
import me.kgustave.dkt.rest.Route

@DktInternal
open class UserImpl
internal constructor(override val bot: DiscordBotImpl, raw: RawUserData, override var untracked: Boolean): User {
    override val id: Long = raw.id
    override val isBot: Boolean = raw.bot
    override var name: String = raw.username
    override var discriminator: String = raw.discriminator
    override var avatarHash: String? = raw.avatar
    override val defaultAvatarHash: String get() = User.DefaultAvatarHashes[discriminator.toInt() % 5]
    override val avatarUrl: String get() {
        avatarHash?.let { avatarHash ->
            val suffix = if(avatarHash.startsWith("a_")) "gif" else "png"
            return "${User.AvatarBaseUrl}/$id/$avatarHash.$suffix"
        }
        return "${User.DefaultAvatarBaseUrl}/$defaultAvatarHash.png"
    }

    internal var privateChannel: PrivateChannelImpl? = null

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        privateChannel?.let { return bot.emptyPromise(it) }
        val body = json { "recipient_id" to "$id" }
        return bot.restPromise(Route.CreateDM, body = body) { call ->
            val rawChannel = call.response.receive<RawChannel>()
            bot.entities.handlePrivateChannel(rawChannel)
        }
    }

    final override fun hashCode(): Int = id.hashCode()
    final override fun equals(other: Any?): Boolean = other is UserImpl && Snowflake.equals(this, other)
    final override fun toString(): String = Snowflake.toString("U", this)
}
