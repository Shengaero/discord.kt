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
package me.kgustave.dkt.entities

import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.entities.cache.MemberCache
import me.kgustave.dkt.entities.cache.SnowflakeCache
import me.kgustave.dkt.requests.RestPromise

interface Guild: Snowflake, ChannelHolder {
    val bot: DiscordBot
    val name: String
    val iconId: String?
    val iconUrl: String?
    val splashId: String?
    val splashUrl: String?
    val owner: Member
    val everyoneRole: Role
    val systemChannel: TextChannel?
    val afkChannel: VoiceChannel?
    val features: Set<String>
    val unavailable: Boolean
    val hasWidget: Boolean
    val hasElevatedMFALevel: Boolean
    val mfaLevel: MFALevel
    val verificationLevel: VerificationLevel
    val defaultNotificationLevel: NotificationLevel
    val explicitContentFilter: ExplicitContentFilter

    val roleCache: SnowflakeCache<Role>
    val emoteCache: SnowflakeCache<GuildEmote>
    val memberCache: MemberCache
    val categoryCache: SnowflakeCache<Category>
    val textChannelCache: SnowflakeCache<TextChannel>
    val voiceChannelCache: SnowflakeCache<VoiceChannel>

    val roles: List<Role>
    val emotes: List<GuildEmote>
    val members: List<Member>
    val categories: List<Category>

    val self: Member

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

    enum class MFALevel {
        NONE,
        TWO_FACTOR_AUTHENTICATION,
        UNKNOWN;
        companion object {
            fun typeOf(ordinal: Int): MFALevel {
                if(ordinal >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { ordinal == it.ordinal } ?: UNKNOWN
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
            fun typeOf(ordinal: Int): VerificationLevel {
                if(ordinal >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { ordinal == it.ordinal } ?: UNKNOWN
            }
        }
    }

    enum class NotificationLevel {
        ALL_MESSAGES,
        ONLY_MENTIONS,
        UNKNOWN;
        companion object {
            fun typeOf(ordinal: Int): NotificationLevel {
                if(ordinal >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { ordinal == it.ordinal } ?: UNKNOWN
            }
        }
    }

    enum class ExplicitContentFilter(val description: String) {
        DISABLED("Don't scan any messages."),
        MEMBERS_WITHOUT_ROLES("Scan messages from members without a role."),
        ALL_MEMBERS("Scan messages sent by all members."),
        UNKNOWN("Unknown contentBuilder filter!");
        companion object {
            fun typeOf(ordinal: Int): ExplicitContentFilter {
                if(ordinal >= UNKNOWN.ordinal)
                    return UNKNOWN

                return values().firstOrNull { ordinal == it.ordinal } ?: UNKNOWN
            }
        }
    }
}
