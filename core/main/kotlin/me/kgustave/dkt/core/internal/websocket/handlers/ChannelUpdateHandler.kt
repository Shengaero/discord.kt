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
package me.kgustave.dkt.core.internal.websocket.handlers

import me.kgustave.dkt.core.entities.Channel
import me.kgustave.dkt.core.entities.Channel.Type.*
import me.kgustave.dkt.core.entities.Member
import me.kgustave.dkt.core.entities.PermissionHolder
import me.kgustave.dkt.core.entities.Role
import me.kgustave.dkt.core.internal.cache.EventCache
import me.kgustave.dkt.core.internal.data.RawChannel
import me.kgustave.dkt.core.internal.data.RawPermissionOverwrite
import me.kgustave.dkt.core.internal.entities.AbstractGuildChannelImpl
import me.kgustave.dkt.core.internal.entities.DiscordBotImpl
import me.kgustave.dkt.core.internal.websocket.DiscordWebSocket
import me.kgustave.dkt.core.internal.websocket.Payload

internal class ChannelUpdateHandler(bot: DiscordBotImpl): WebSocketHandler(bot) {
    override fun handle(payload: Payload) {
        val channel = payload.d as RawChannel

        val id = channel.id

        when(val type = Channel.Type.of(channel.type)) {
            UNKNOWN -> return

            TEXT -> {
                val textChannel = bot.textChannelCache[id]

                if(textChannel == null) {
                    bot.eventCache.cache(EventCache.Type.CHANNEL, id, payload, this::handle)
                    return logUnCachedEntity("CHANNEL_UPDATE", "text channel", id)
                }

                val oldName = textChannel.name
                val oldParentId = textChannel.parentId
                val oldPosition = textChannel.rawPosition
                val oldTopic = textChannel.topic
                val oldNsfw = textChannel.nsfw
                val oldRateLimitPerUser = textChannel.rateLimitPerUser

                if(channel.name != oldName) {
                    textChannel.name = channel.name
                    // TODO Event
                }

                if(channel.parentId != oldParentId) {
                    textChannel.parentId = channel.parentId
                    // TODO Event
                }

                if(channel.position != oldPosition && channel.position != null) {
                    textChannel.rawPosition = channel.position
                    // TODO Event
                }

                if(channel.topic != oldTopic) {
                    textChannel.topic = channel.topic
                    // TODO Event
                }

                if(channel.nsfw != oldNsfw) {
                    textChannel.nsfw = channel.nsfw
                    // TODO Event
                }

                if(channel.rateLimitPerUser != oldRateLimitPerUser) {
                    textChannel.rateLimitPerUser = channel.rateLimitPerUser ?: 0
                    // TODO Event
                }

                val changedPermissionHolders = applyPermissions(payload, textChannel, channel)

                if(changedPermissionHolders.isNotEmpty()) {
                    // TODO Event
                }
            }

            VOICE -> {
                val voiceChannel = bot.voiceChannelCache[id]

                if(voiceChannel == null) {
                    bot.eventCache.cache(EventCache.Type.CHANNEL, id, payload, this::handle)
                    return logUnCachedEntity("CHANNEL_UPDATE", "voice channel", id)
                }

                val oldName = voiceChannel.name
                val oldParentId = voiceChannel.parentId
                val oldPosition = voiceChannel.rawPosition
                val oldBitrate = voiceChannel.bitrate
                val oldUserLimit = voiceChannel.userLimit

                if(channel.name != oldName) {
                    voiceChannel.name = channel.name
                    // TODO Event
                }

                if(channel.parentId != oldParentId) {
                    voiceChannel.parentId = channel.parentId
                    // TODO Event
                }

                if(channel.position != oldPosition && channel.position != null) {
                    voiceChannel.rawPosition = channel.position
                    // TODO Event
                }

                if(channel.bitrate != oldBitrate && channel.bitrate != null) {
                    voiceChannel.bitrate = channel.bitrate
                    // TODO Event
                }

                if(channel.userLimit != oldUserLimit && channel.userLimit != null) {
                    voiceChannel.userLimit = channel.userLimit
                    // TODO Event
                }

                val changedPermissionHolders = applyPermissions(payload, voiceChannel, channel)

                if(changedPermissionHolders.isNotEmpty()) {
                    // TODO Event
                }
            }

            CATEGORY -> {
                val category = bot.categoryCache[id]

                if(category == null) {
                    bot.eventCache.cache(EventCache.Type.CHANNEL, id, payload, this::handle)
                    return logUnCachedEntity("CHANNEL_UPDATE", "category", id)
                }

                val oldName = category.name
                val oldPosition = category.rawPosition

                if(channel.name != oldName) {
                    category.name = channel.name
                    // TODO Event
                }

                if(channel.position != oldPosition && channel.position != null) {
                    category.rawPosition = channel.position
                }

                val changedPermissionHolders = applyPermissions(payload, category, channel)

                if(changedPermissionHolders.isNotEmpty()) {
                    // TODO Event
                }
            }

            else -> error("Invalid channel type for CHANNEL_UPDATE: $type")
        }
    }

    // returns a list of changed perms if they did change, or else an empty list
    private fun applyPermissions(
        payload: Payload,
        guildChannel: AbstractGuildChannelImpl,
        channel: RawChannel
    ): List<PermissionHolder> {
        val changed = arrayListOf<PermissionHolder>()
        val all = arrayListOf<PermissionHolder>()

        val guild = guildChannel.guild
        val permissionOverrides = guildChannel.permissionOverrides

        for(overwrite in channel.permissionOverwrites) {
            applyPermissionOverride(payload, overwrite, guildChannel, channel, changed, all)
        }

        // Move the IDs over to a separate collection to avoid concurrent modification
        //while iterating over the elements.
        permissionOverrides.keys.toList().asSequence()
            .mapNotNull { id -> (guild.getRoleById(id) ?: guild.getMemberById(id)) as? PermissionHolder }
            .filter { it !in all }
            .forEach { holder ->
                changed += holder
                permissionOverrides -= (holder as? Role)?.id ?: (holder as Member).user.id
            }

        return changed
    }

    private fun applyPermissionOverride(
        payload: Payload,
        overwrite: RawPermissionOverwrite,
        guildChannel: AbstractGuildChannelImpl,
        channel: RawChannel,
        changed: MutableList<PermissionHolder>,
        all: MutableList<PermissionHolder>
    ) {
        val id = overwrite.id

        val holder: PermissionHolder = when(overwrite.type.toLowerCase()) {
            "role" -> guildChannel.guild.getRoleById(id) ?: run {
                bot.eventCache.cache(EventCache.Type.ROLE, id, payload) {
                    applyPermissionOverride(payload, overwrite, guildChannel, channel, changed, all)
                }

                return EventCache.Log.debug(
                    "Received CHANNEL_UPDATE event with new or updated " +
                    "permission overwrite for a role that was not cached or does not exist (ID: $id)"
                )
            }

            "member" -> guildChannel.guild.getMemberById(id) ?: run {
                bot.eventCache.cache(EventCache.Type.MEMBER, id, payload) {
                    applyPermissionOverride(payload, overwrite, guildChannel, channel, changed, all)
                }

                return EventCache.Log.debug(
                    "Received CHANNEL_UPDATE event with new or updated " +
                    "permission overwrite for a member that was not cached or does not exist (ID: $id)"
                )
            }

            else -> return DiscordWebSocket.Log.warn("Received unrecognized permission overwrite type: ${overwrite.type}")
        }

        val override = guildChannel.permissionOverrides[id]

        if(override == null) { // created
            bot.entities.handlePermissionOverwrite(overwrite, guildChannel, holder)
            changed += holder
        } else {
            override.rawAllowed = overwrite.allow.toLong()
            override.rawDenied = overwrite.deny.toLong()
            changed += holder
        }
        all += holder
    }
}
