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
import me.kgustave.dkt.entities.cache.MemberCache
import me.kgustave.dkt.entities.cache.SnowflakeCache
import me.kgustave.dkt.managers.VoiceManager
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.voice.ExperimentalVoiceAPI

interface Guild: Snowflake, ChannelHolder {
    val bot: DiscordBot
    val name: String
    val iconHash: String?
    val iconUrl: String?
    val splashHash: String?
    val splashUrl: String?
    val owner: Member
    val publicRole: Role
    val systemChannel: TextChannel?
    val afkChannel: VoiceChannel?
    val features: List<String>
    val unavailable: Boolean
    val hasWidget: Boolean
    val hasElevatedMFALevel: Boolean
    val mfaLevel: MFALevel
    val verificationLevel: VerificationLevel
    val defaultNotificationLevel: NotificationLevel
    val explicitContentFilter: ExplicitContentFilter

    val roleCache: SnowflakeCache<out Role>
    val emojiCache: SnowflakeCache<out GuildEmoji>
    val memberCache: MemberCache
    val categoryCache: SnowflakeCache<out Category>
    val textChannelCache: SnowflakeCache<out TextChannel>
    val voiceChannelCache: SnowflakeCache<out VoiceChannel>

    val roles: List<Role>
    val emojis: List<GuildEmoji>
    val members: List<Member>
    val categories: List<Category>

    /** Gets the [Member] representing the currently logged in account. */
    val self: Member

    /**
     * The voice manager for this guild.
     *
     * This API is experimental, and currently unimplemented.
     */
    @ExperimentalVoiceAPI
    val voiceManager: VoiceManager get() = TODO("not implemented")

    fun getCategoriesByName(name: String, ignoreCase: Boolean = false): List<Category>
    fun getTextChannelsByName(name: String, ignoreCase: Boolean = false): List<TextChannel>
    fun getVoiceChannelsByName(name: String, ignoreCase: Boolean = false): List<VoiceChannel>

    fun getCategoryById(id: Long): Category?
    fun getTextChannelById(id: Long): TextChannel?
    fun getVoiceChannelById(id: Long): VoiceChannel?

    fun getMember(user: User): Member?

    fun getMemberById(id: Long): Member?
    fun getMembersByName(name: String, ignoreCase: Boolean = false): List<Member>
    fun getMembersByUsername(name: String, ignoreCase: Boolean = false): List<Member>
    fun getMembersByNickname(name: String, ignoreCase: Boolean = false): List<Member>

    fun getRoleById(id: Long): Role?
    fun getRolesByName(name: String, ignoreCase: Boolean = false): List<Role>

    /**
     * Leaves the [Guild].
     *
     * @return A [RestPromise] to leave the Guild.
     */
    fun leave(): RestPromise<Unit>

    @Suppress("DEPRECATION")
    @Deprecated("GuildEmote is now deprecated in favor of GuildEmoji")
    val emoteCache: SnowflakeCache<out GuildEmote>

    @Suppress("DEPRECATION")
    @Deprecated("GuildEmote is now deprecated in favor of GuildEmoji")
    val emotes: List<GuildEmote>

    enum class MFALevel {
        NONE,
        TWO_FACTOR_AUTHENTICATION,
        UNKNOWN;
        companion object {
            fun of(type: Int): MFALevel {
                if(type >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { type == it.ordinal } ?: UNKNOWN
            }
        }
    }

    enum class VerificationLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        UNKNOWN;

        companion object {
            fun of(type: Int): VerificationLevel {
                if(type >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { type == it.ordinal } ?: UNKNOWN
            }
        }
    }

    enum class NotificationLevel {
        ALL_MESSAGES,
        ONLY_MENTIONS,
        UNKNOWN;
        companion object {
            fun of(type: Int): NotificationLevel {
                if(type >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { type == it.ordinal } ?: UNKNOWN
            }
        }
    }

    enum class ExplicitContentFilter(val description: String) {
        DISABLED("Don't scan any messages."),
        MEMBERS_WITHOUT_ROLES("Scan messages from members without a role."),
        ALL_MEMBERS("Scan messages sent by all members."),
        UNKNOWN("Unknown contentBuilder filter!");
        companion object {
            fun of(type: Int): ExplicitContentFilter {
                if(type >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { type == it.ordinal } ?: UNKNOWN
            }
        }
    }
}
