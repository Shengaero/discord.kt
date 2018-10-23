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
@file:Suppress("MemberVisibilityCanBePrivate")

package me.kgustave.dkt.internal.impl

import io.ktor.client.call.receive
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.exceptions.RequestException
import me.kgustave.dkt.exceptions.UnloadedPropertyException
import me.kgustave.dkt.internal.cache.MemberCacheImpl
import me.kgustave.dkt.internal.cache.SnowflakeCacheImpl
import me.kgustave.dkt.internal.cache.SortableSnowflakeCache
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.promises.restPromise
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.util.delegates.weak

internal class GuildImpl(override val id: Long, bot: DiscordBotImpl, override var unavailable: Boolean = true): Guild {
    companion object {
        // 1: ID, 2: hash
        private const val IconUrlFormat   = "${Requester.CDNBaseUrl}/icons/%d/%s.png"
        private const val SplashUrlFormat = "${Requester.CDNBaseUrl}/splashes/%d/%s.png"
    }

    override val bot: DiscordBotImpl by weak(bot)

    private lateinit var _name: String
    private lateinit var _publicRole: Role
    private lateinit var _features: List<String>
    private lateinit var _mfaLevel: Guild.MFALevel
    private lateinit var _verificationLevel: Guild.VerificationLevel
    private lateinit var _defaultNotificationLevel: Guild.NotificationLevel
    private lateinit var _explicitContentFilter: Guild.ExplicitContentFilter

    internal var ownerId: Long? = null

    override var name: String
        set(value) { _name = value }
        get() {
            checkAvailable()
            return _name
        }
    override var publicRole: Role
        set(value) { _publicRole = value }
        get() {
            checkAvailable()
            return _publicRole
        }
    override var features: List<String>
        set(value) { _features = value }
        get() {
            checkAvailable()
            return _features
        }
    override var mfaLevel: Guild.MFALevel
        set(value) { _mfaLevel = value }
        get() {
            checkAvailable()
            return _mfaLevel
        }
    override var verificationLevel: Guild.VerificationLevel
        set(value) { _verificationLevel = value }
        get() {
            checkAvailable()
            return _verificationLevel
        }
    override var defaultNotificationLevel: Guild.NotificationLevel
        set(value) { _defaultNotificationLevel = value }
        get() {
            checkAvailable()
            return _defaultNotificationLevel
        }
    override var explicitContentFilter: Guild.ExplicitContentFilter
        set(value) { _explicitContentFilter = value }
        get() {
            checkAvailable()
            return _explicitContentFilter
        }
    override var systemChannel: TextChannel? = null
        get() {
            checkAvailable()
            return field
        }
    override var afkChannel: VoiceChannel? = null
        get() {
            checkAvailable()
            return field
        }
    override var iconHash: String? = null
        get() {
            checkAvailable()
            return field
        }
    override var splashHash: String? = null
        get() {
            checkAvailable()
            return field
        }
    override var hasWidget: Boolean = false
        get() {
            checkAvailable()
            return field
        }
    override var hasElevatedMFALevel: Boolean = false
        get() {
            checkAvailable()
            return field
        }

    override val owner: Member get() {
        checkAvailable()
        return ownerId?.let { memberCache[it] } ?: throw UnloadedPropertyException("Owner was not loaded!")
    }

    override val self: Member get() {
        checkAvailable()
        return getMember(bot.self) ?: throw UnloadedPropertyException(
            "Member for self not loaded (Guild was left before created?!)"
        )
    }

    override val iconUrl: String? get() {
        checkAvailable()
        return iconHash?.let { IconUrlFormat.format(id, it) }
    }

    override val splashUrl: String? get() {
        checkAvailable()
        return splashHash?.let { SplashUrlFormat.format(id, it) }
    }

    override val roleCache = SortableSnowflakeCache(RoleImpl::name, Comparator.reverseOrder())
    override val emoteCache = SnowflakeCacheImpl(GuildEmoteImpl::name)
    override val memberCache = MemberCacheImpl()
    override val categoryCache = SortableSnowflakeCache(CategoryImpl::name, Comparator.naturalOrder())
    override val textChannelCache = SortableSnowflakeCache(TextChannelImpl::name, Comparator.naturalOrder())
    override val voiceChannelCache = SortableSnowflakeCache(VoiceChannelImpl::name, Comparator.naturalOrder())

    override val roles: List<Role>                 get() = roleCache.toList()
    override val emotes: List<GuildEmote>          get() = emoteCache.toList()
    override val members: List<Member>             get() = memberCache.toList()
    override val categories: List<Category>        get() = categoryCache.toList()
    override val textChannels: List<TextChannel>   get() = textChannelCache.toList()
    override val voiceChannels: List<VoiceChannel> get() = voiceChannelCache.toList()

    override fun getCategoriesByName(name: String, ignoreCase: Boolean): List<Category> {
        return categoryCache.getByName(name, ignoreCase)
    }

    override fun getTextChannelsByName(name: String, ignoreCase: Boolean): List<TextChannel> {
        return textChannelCache.getByName(name, ignoreCase)
    }

    override fun getVoiceChannelsByName(name: String, ignoreCase: Boolean): List<VoiceChannel> {
        return voiceChannelCache.getByName(name, ignoreCase)
    }

    override fun getCategoryById(id: Long): Category? = categoryCache[id]

    override fun getTextChannelById(id: Long): TextChannel? = textChannelCache[id]

    override fun getVoiceChannelById(id: Long): VoiceChannel? = voiceChannelCache[id]

    override fun getMember(user: User): Member? = getMemberById(user.id)

    override fun getMemberById(id: Long): Member? = memberCache[id]

    override fun getMembersByName(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByName(name, ignoreCase)
    }

    override fun getMembersByUsername(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByUsername(name, ignoreCase)
    }

    override fun getMembersByNickname(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByNickname(name, ignoreCase)
    }

    override fun getRoleById(id: Long): Role? {
        return roleCache[id]
    }

    override fun getRolesByName(name: String, ignoreCase: Boolean): List<Role> {
        return roleCache.getByName(name, ignoreCase)
    }

    override fun leave(): RestPromise<Unit> = bot.restPromise(Route.LeaveGuild.format(id)) { call ->
        // 204 -> Successfully left guild
        if(call.response.status.value != 204) {
            throw call.response.receive<RequestException>()
        }
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Guild && Snowflake.equals(this, other)
    override fun toString(): String = Snowflake.toString("Guild", this)

    private fun checkAvailable() {
        if(unavailable)
            throw UnloadedPropertyException("Could not get property for unloaded Guild (ID: $id)")
    }
}
