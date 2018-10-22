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
@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis")
package me.kgustave.dkt.internal.impl

import me.kgustave.dkt.entities.Channel
import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.Role
import me.kgustave.dkt.internal.cache.EventCache
import me.kgustave.dkt.internal.data.*
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.delegates.weak

internal class EntityHandler(bot: DiscordBotImpl) {
    private val bot by weak(bot)

    fun handleSelfUser(raw: RawSelfUser): SelfUserImpl {
        return when {
            // already initialized self user
            bot.selfIsInit() -> bot.self.also {
                // patch in new data
                it.patch(raw)
            }

            // need to create and cache self user
            else -> SelfUserImpl(bot, raw).also {
                // initialize self
                bot.self = it
                // cache
                bot.userCache[it.id] = it
            }
        }
    }

    fun handleUser(raw: RawUser, modifyCache: Boolean = true): UserImpl {
        var impl = bot.userCache[raw.id]

        if(impl != null) impl.patch(raw) else {
            impl = UserImpl(bot, raw)
            if(modifyCache) {
                bot.userCache[impl.id] = impl
            }
        }

        return impl
    }

    fun handleGuild(raw: RawGuild, members: Map<Long, RawMember>): GuildImpl {
        val id = raw.id
        val guild = bot.guildCache[id] ?: GuildImpl(id, bot)

        with(guild) {
            unavailable = false
            name = raw.name
            iconHash = raw.icon
            splashHash = raw.splash
            ownerId = raw.ownerId
            features = raw.features
            defaultNotificationLevel = Guild.NotificationLevel.of(raw.defaultMessageNotifications)
            verificationLevel = Guild.VerificationLevel.of(raw.verificationLevel)
            explicitContentFilter = Guild.ExplicitContentFilter.of(raw.explicitContentFilter)
            // TODO region = raw.region
            // TODO afkTimeout = ...
            // TODO Further setup
        }

        members.values.forEach { member -> handleMember(member, guild) }

        raw.channels.forEach { channel -> handleGuildChannel(channel, guild) }

        raw.voiceStates.forEach { voiceState -> handleVoiceState(voiceState, guild) }

        return guild
    }

    fun handleGuildChannel(raw: RawChannel, guild: GuildImpl): AbstractGuildChannelImpl {
        return when(val type = Channel.Type.of(raw.type)) {
            Channel.Type.TEXT -> handleTextChannel(raw, guild.id, guild)
            Channel.Type.VOICE -> handleVoiceChannel(raw, guild.id, guild)
            Channel.Type.CATEGORY -> handleCategory(raw, guild.id, guild)
            else -> throw IllegalArgumentException("Invalid channel type $type")
        }
    }

    fun handleTextChannel(raw: RawChannel, guildId: Long, guild: GuildImpl? = bot.guildCache[guildId]): TextChannelImpl {
        var playCache = false
        val id = raw.id
        val channel = bot.textChannelCache.computeIfAbsent(id) c@ {
            checkNotNull(guild) {
                "While creating text channel for guild (ID: $guildId), " +
                "could not find any cached guild!"
            }
            playCache = true
            val text = TextChannelImpl(id, guild)
            guild.textChannelCache[id] = text
            return@c text
        }

        val overwrites = raw.permissionOverwrites
        if(overwrites.isNotEmpty()) {
            // TODO Handle Overrides
        }

        with(channel) {
            name = raw.name
            parentId = raw.parentId
            // TODO lastMessageId = raw.lastMessageId
            // TODO position = raw.position
            // TODO rateLimitPerUser = raw.rateLimitPerUser
            // TODO nsfw = raw.nsfw
            // TODO topic = raw.topic
        }

        if(playCache) bot.eventCache.play(EventCache.Type.CHANNEL, id)
        return channel
    }

    fun handleVoiceChannel(raw: RawChannel, guildId: Long, guild: GuildImpl? = bot.guildCache[guildId]): VoiceChannelImpl {
        var playCache = false
        val id = raw.id
        val channel = bot.voiceChannelCache.computeIfAbsent(id) c@ {
            checkNotNull(guild) {
                "While creating voice channel for guild (ID: $guildId), " +
                "could not find any cached guild!"
            }
            playCache = true
            val voice = VoiceChannelImpl(id, guild)
            guild.voiceChannelCache[id] = voice
            return@c voice
        }

        val overwrites = raw.permissionOverwrites
        if(overwrites.isNotEmpty()) {
            // TODO Handle Overrides
        }

        with(channel) {
            name = raw.name
            parentId = raw.parentId
            // TODO handle other things
        }

        if(playCache) bot.eventCache.play(EventCache.Type.CHANNEL, id)
        return channel
    }

    fun handleCategory(raw: RawChannel, guildId: Long, guild: GuildImpl? = bot.guildCache[guildId]): CategoryImpl {
        var playCache = false
        val id = raw.id
        val channel = bot.categoryCache.computeIfAbsent(id) c@ {
            checkNotNull(guild) {
                "While creating category for guild (ID: $guildId), " +
                "could not find any cached guild!"
            }
            playCache = true
            val category = CategoryImpl(id, guild)
            guild.categoryCache[id] = category
            return@c category
        }

        val overwrites = raw.permissionOverwrites
        if(overwrites.isNotEmpty()) {
            // TODO Handle Overrides
        }

        with(channel) {
            name = raw.name
            // TODO handle other things
        }

        if(playCache) bot.eventCache.play(EventCache.Type.CHANNEL, id)
        return channel
    }

    fun handleRole(raw: RawRole, guildId: Long): RoleImpl = handleRole(raw, checkNotNull(bot.guildCache[guildId]))

    fun handleRole(raw: RawRole, guild: GuildImpl): RoleImpl {
        var playCache = false
        val id = raw.id
        val role = guild.roleCache.computeIfAbsent(id) {
            playCache = true
            RoleImpl(guild, id)
        }

        with(role) {
            name = raw.name
            colorInt = raw.color.takeIf { it == 0 } ?: Role.DefaultColorInt
            rawPosition = raw.position
            rawPermissions = raw.permissions.toLong()
        }

        // we are ready to play the cached events for this role
        if(playCache) bot.eventCache.play(EventCache.Type.ROLE, id)
        return role
    }

    fun handleMember(raw: RawMember, guild: GuildImpl): MemberImpl {
        val user = handleUser(raw.user)

        // member already exists
        (guild.memberCache[user.id] as? MemberImpl)?.let { member -> return member }

        val member = MemberImpl(guild, user)

        guild.memberCache[user.id] = member

        return member
    }

    fun handleVoiceState(raw: RawVoiceState, guild: GuildImpl): GuildVoiceStateImpl? {
        val userId = raw.userId
        val member = guild.memberCache[userId] as? MemberImpl

        if(member == null) {
            Log.warn("Received call to create voice state for unknown member " +
                     "of guild (GuildID: ${guild.id}, UserId: $userId)")
            return null
        }

        if(!member.voiceStateIsInit()) return null

        val voiceState = member.voiceState
        val voice = guild.voiceChannelCache[raw.channelId]

        if(voice != null) voice.connectedMembers[member.user.id] = member else {
            Log.warn("Received call to create voice state for unknown channel " +
                     "of guild (GuildID: ${guild.id}, ChannelId: ${raw.channelId})")
        }

        with(voiceState) {
            mute = raw.mute
            deaf = raw.deaf
            selfMute = raw.selfMute
            selfDeaf = raw.selfDeaf
            suppress = raw.suppress
            sessionId = raw.sessionId
            channel = voice
        }

        return voiceState
    }

    companion object {
        val Log = createLogger(EntityHandler::class)
    }
}
