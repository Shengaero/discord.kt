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
import me.kgustave.dkt.rest.DiscordRequester
import me.kgustave.dkt.promises.RestPromise

/**
 * Represents a Discord [User](https://discordapp.com/developers/docs/resources/user).
 *
 * These entities are tracked by a [DiscordBot] via a couple of distinct sources,
 * however in most cases they are classifiable into two general kinds:
 *
 * 1) **Tracked** - These user instances are tracked by the bot, they are cached completely,
 * and updated when discord provides information pertaining to changes to the user.
 *
 * 2) **Untracked** - These user instances are not tracked by the bot, they are
 * cached lightly if possible, but updates are made very rarely (possibly never).
 */
interface User: Snowflake, Mentionable {
    /** The bot instance responsible for the creation and tracking of this User instance. */
    val bot: DiscordBot

    /** The user's discord name, between 2 and 32 characters long. */
    val name: String

    /** The user's 4 digit discriminator. */
    val discriminator: String

    /** The users's avatar hash, or `null` if the user has not set their account's avatar. */
    val avatarHash: String?

    /**
     * The user's default avatar hash, which corresponds to the value at the index
     * from the set of [default avatar hashes][DefaultAvatarHashes], calculated
     * via the following formula: `index = discriminator % 5`
     */
    val defaultAvatarHash: String

    /**
     * The user's avatar url, either one based on a present [avatarHash],
     * or the one based on the user's [defaultAvatarHash].
     *
     * To check if a user has an avatar set, try checking to see if
     * [avatarHash] is null.
     */
    val avatarUrl: String

    /** Whether or not this user is a bot. */
    val isBot: Boolean

    /**
     * Whether or not this user is tracked by the bot, meaning it
     * will be consistently updated when discord tells us information
     * about it has changed.
     */
    val untracked: Boolean

    /** The user as a mention, in the format: `<@USER_ID>` */
    override val mention: String get() = "<@$id>"

    /**
     * Opens a [PrivateChannel] with the user, returning a [RestPromise]
     * for the opened [PrivateChannel].
     */
    fun openPrivateChannel(): RestPromise<PrivateChannel>

    companion object {
        /** The CDN base URL for user avatars. */
        const val AvatarBaseUrl = "${DiscordRequester.CDNBaseUrl}/avatars"

        /** The CDN base URL for default user avatars. */
        const val DefaultAvatarBaseUrl = "${DiscordRequester.CDNBaseUrl}/embed/avatars"

        /**
         * A set of default user avatar hashes.
         *
         * The default avatar of a user can be selected from this array
         * based on the index calculated from the formula described in
         * the documentation for [User.defaultAvatarHash].
         *
         * @see User.defaultAvatarHash
         */
        val DefaultAvatarHashes = arrayOf(
            "6debd47ed13483642cf09e832ed0bc1b", // blurple
            "322c936a8c8be1b803cd94861bdfa868", // gray
            "dd4dbc0016779df1378e7812eabaa04d", // green
            "0e291f67c9274a1abdddeb3fd919cbaa", // orange
            "1cbd08c76f8af6dddce02c5138971129"  // red
        )
    }
}
