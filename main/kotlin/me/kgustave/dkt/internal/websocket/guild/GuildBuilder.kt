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
package me.kgustave.dkt.internal.websocket.guild

import me.kgustave.dkt.internal.cache.EventCache
import me.kgustave.dkt.internal.data.RawGuild
import me.kgustave.dkt.internal.data.RawGuildData
import me.kgustave.dkt.internal.data.RawMember
import me.kgustave.dkt.internal.data.RawUnavailableGuild
import me.kgustave.dkt.internal.websocket.Payload
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

internal class GuildBuilder(val id: Long, val manager: GuildSetupManager, val join: Boolean) {

    private lateinit var members: MutableMap<Long, RawMember>
    private lateinit var membersToRemove: MutableSet<Long>
    private val eventQueue = LinkedList<Payload>()
    private var raw: RawGuildData? = null

    var firedUnavailableJoin = false
    var chunkingRequested = false
    var unavailable = false
    var status = Status.INIT
    var expectedSize = 1 // all guilds should have at least 1 member
        set(value) { field = max(1, value) }

    operator fun contains(userId: Long) = userId in members

    fun reset() {
        status = Status.UNAVAILABLE
        expectedSize = 1
        raw = null
        chunkingRequested = false
        if(::members.isInitialized) {
            members.clear()
        }
        if(::membersToRemove.isInitialized) {
            membersToRemove.clear()
        }
        eventQueue.clear()
    }

    fun create(raw: RawGuildData) {
        this.raw = raw
        this.unavailable = raw.unavailable

        when {
            raw is RawUnavailableGuild || unavailable -> {
                if(!firedUnavailableJoin && join) {
                    firedUnavailableJoin = true
                    // TODO Fire unavailable join event
                }
                return
            }

            raw is RawGuild -> {
                expectedSize = checkNotNull(raw.memberCount)
                members = HashMap(expectedSize)

                handleMembers()
            }
        }
    }

    fun handleAddMember(raw: RawMember) {
        if(!::members.isInitialized || !::membersToRemove.isInitialized) {
            return
        }
        expectedSize++
        val id = raw.user.id
        members[id] = raw
        membersToRemove.remove(id)
    }

    fun handleRemoveMember(raw: RawMember) {
        if(!::members.isInitialized || !::membersToRemove.isInitialized)
            expectedSize++
        val id = raw.user.id
        members.remove(id)
        membersToRemove.add(id)
        val eventCache = manager.bot.eventCache
        if(manager.contains(this, id)) {
            eventCache.clear(EventCache.Type.USER, id)
        }
    }

    fun handleMemberChunk(members: List<RawMember>): Boolean {
        if(raw == null) {
            GuildSetupManager.Log.debug("Dropping chunk for unavailable guild!")
            return true
        }

        for(member in members) {
            val id = member.user.id
            this.members[id] = member
        }

        if(members.size >= expectedSize) {
            complete()
            return false
        }

        return true
    }

    fun cache(event: Payload) {
        eventQueue += event
        // TODO handle possible infinite event accumulation
    }

    fun destroy() {
        status = Status.REMOVED
        val eventCache = manager.bot.eventCache
        eventCache.clear(EventCache.Type.GUILD, id)
        val raw = raw as? RawGuild ?: return

        for(channel in raw.channels) {
            eventCache.clear(EventCache.Type.CHANNEL, channel.id)
        }

        for(role in raw.roles) {
            eventCache.clear(EventCache.Type.ROLE, role.id)
        }

        if(::members.isInitialized) {
            for((userId) in members) {
                if(manager.contains(this, userId)) continue
                eventCache.clear(EventCache.Type.USER, userId)
            }
        }
    }

    private fun complete() {
        val raw = checkNotNull(raw as? RawGuild) { "Got call to build when raw guild was null!" }
        status = Status.BUILDING
        manager.bot.entities.handleGuild(raw, members)

        // TODO Events
        if(!join) manager.guildReady(id) else {
            if(chunkingRequested) {
                manager.guildReady(id)
            } else {
                manager.guildRemove(id)
            }
        }

        status = Status.READY
        eventQueue.forEach { payload -> manager.bot.websocket.handleEvent(payload) }
        manager.bot.eventCache.play(EventCache.Type.GUILD, id)
    }

    private fun handleMembers() {
        val raw = checkNotNull(this.raw as? RawGuild)

        if(members.size <= expectedSize && !chunkingRequested) {
            status = Status.CHUNKING
            manager.addChunking(id, join)
            chunkingRequested = true
        }

        if(handleMemberChunk(raw.members) && !chunkingRequested) {
            status = Status.CHUNKING
            manager.addChunking(id, join)
            chunkingRequested = true
        }
    }

    enum class Status {
        INIT,
        CHUNKING,
        BUILDING,
        READY,
        UNAVAILABLE,
        REMOVED;
    }
}
