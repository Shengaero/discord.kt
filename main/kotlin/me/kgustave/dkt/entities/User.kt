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
@file:Suppress("unused")
package me.kgustave.dkt.entities

import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.RestPromise

interface User: Snowflake, Mentionable {
    val bot: DiscordBot
    val name: String
    val discriminator: Int
    val avatarHash: String?
    val defaultAvatarHash: String
    val avatarUrl: String
    val isBot: Boolean

    override val mention: String get() = "<@$id>"

    fun openPrivateChannel(): RestPromise<PrivateChannel>

    companion object {
        const val AvatarBaseUrl = "${Requester.CDNBaseUrl}/avatars"
        const val DefaultAvatarBaseUrl = "${Requester.CDNBaseUrl}/embed/avatars"
        val DefaultAvatarHashes = arrayOf(
            "6debd47ed13483642cf09e832ed0bc1b", // blurple
            "322c936a8c8be1b803cd94861bdfa868", // gray
            "dd4dbc0016779df1378e7812eabaa04d", // green
            "0e291f67c9274a1abdddeb3fd919cbaa", // orange
            "1cbd08c76f8af6dddce02c5138971129"  // red
        )

        @Deprecated(
            message = "Renamed to be more clear on values stored.",
            replaceWith = ReplaceWith("User.DefaultAvatarHashes")
        )
        val DefaultAvatars = DefaultAvatarHashes
    }
}
