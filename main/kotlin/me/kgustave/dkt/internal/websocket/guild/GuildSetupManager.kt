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
package me.kgustave.dkt.internal.websocket.guild

import kotlinx.coroutines.*
import me.kgustave.dkt.internal.data.RawGuildData
import me.kgustave.dkt.internal.data.RawMember
import me.kgustave.dkt.internal.data.RawUnavailableGuild
import me.kgustave.dkt.internal.impl.DiscordBotImpl
import me.kgustave.dkt.internal.websocket.Payload
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.currentTimeMs
import java.util.*

internal class GuildSetupManager(val bot: DiscordBotImpl): AutoCloseable {
    private val builders = hashMapOf<Long, GuildBuilder>()
    private val pending = hashMapOf<Long, Long>()
    private val chunking = hashSetOf<Long>()

    private var toComplete = 0

    private val dispatcher = newSingleThreadContext("GuildBuilder Dispatcher")

    private lateinit var worker: Job

    fun setToCompleteAndIsReady(toComplete: Int): Boolean {
        Log.debug("Set number of guilds to complete: $toComplete")
        this.toComplete = toComplete

        if(toComplete == 0) {
            bot.websocket.finishReadyOperations()
            return false
        }

        setupWorker()
        return true
    }

    fun ready(raw: RawUnavailableGuild) {
        Log.debug("Beginning setup for guild ID: ${raw.id}")
        builders.computeIfAbsent(raw.id) { id -> GuildBuilder(id, this, false) }
    }

    fun create(raw: RawGuildData) {
        val builder = builders.computeIfAbsent(raw.id) { id -> GuildBuilder(id, this, true) }
        builder.create(raw)
    }

    fun delete(raw: RawUnavailableGuild) {
        val (id, unavailable) = raw
        val builder = builders[id] ?: return
        Log.debug("Received deletion of guild ID: $id")
        if(unavailable) {
            if(!builder.unavailable && !builder.chunkingRequested) {
                builder.unavailable = true
                if(toComplete > 0) {
                    toComplete--
                    performChunking()
                }
            }
            builder.reset()
        } else {
            builder.destroy()
            if(builder.join && !builder.chunkingRequested) {
                builders.remove(id)
            } else {
                guildReady(id)
            }
        }
    }

    fun chunk(guildId: Long, chunk: List<RawMember>) {
        Log.debug("Received guild member chunk for guild ID: $guildId (${chunk.size} members)")
        synchronized(pending) { pending -= guildId }
        builders[guildId]?.handleMemberChunk(chunk)
    }

    fun addMember(guildId: Long, raw: RawMember): Boolean {
        val builder = builders[guildId] ?: return false
        builder.handleAddMember(raw)
        return true
    }

    fun removeMember(guildId: Long, raw: RawMember): Boolean {
        val builder = builders[guildId] ?: return false
        builder.handleRemoveMember(raw)
        return true
    }

    fun cache(guildId: Long, event: Payload) {
        builders[guildId]?.let {
            return it.cache(event)
        }
        Log.warn("Attempted to cache event for a guild not currently in the builder manager!")
    }

    fun addChunking(guildId: Long, join: Boolean) {
        if(join || toComplete <= 0) {
            if(toComplete <= 0) {
                sendChunkRequest(guildId)
                return
            }
            toComplete++
        }
        chunking += guildId
        performChunking()
    }

    fun contains(builder: GuildBuilder, userId: Long): Boolean {
        for(entry in builders) {
            if(entry.value == builder) continue
            if(userId in entry.value) return true
        }
        return false
    }

    fun clear() {
        builders.clear()
        chunking.clear()
        toComplete = 0
        close()
        synchronized(pending) {
            pending.clear()
        }
    }

    fun guildReady(guildId: Long) {
        Log.debug("Guild is ready: $guildId")
        guildRemove(guildId)
        val websocket = bot.websocket
        if(!websocket.isReady && --toComplete < 1) {
            bot.websocket.finishReadyOperations()
        } else {
            performChunking()
        }
    }

    fun guildRemove(guildId: Long) {
        builders.remove(guildId)
    }

    private fun sendChunkRequest(guildId: Long) = sendChunkRequest(listOf(guildId))

    private fun sendChunkRequest(guildIds: List<Long>) {
        require(guildIds.size <= 50) { "Cannot send more than 50 guilds per chunk request!" }
        Log.debug("Sending chunk request for ${guildIds.size} guilds!")
        val timeout = currentTimeMs + ChunkTimeout
        synchronized(pending) {
            guildIds.associateWithTo(pending) { timeout }
        }
        bot.websocket.sendGuildMemberRequest(Payload.GuildMemberRequest(
            guildId = guildIds,
            query = "", limit = 0)
        )
    }

    private fun performChunking() {
        if(chunking.size >= 50) {
            val chunk = arrayListOf<Long>()
            for(id in chunking.take(50)) {
                chunk += id
                chunking -= id
            }
            sendChunkRequest(chunk)
        }

        if(toComplete > 0 && toComplete == chunking.size) {
            val chunk = chunking.toList()
            chunking.clear()
            sendChunkRequest(chunk)
        }
    }

    private fun setupWorker() {
        worker = GlobalScope.launch(dispatcher) {
            loop@ while(isActive) {
                delay(ChunkTimeout)
                if(pending.isEmpty()) break
                synchronized(pending) {
                    val requests = pending.iterator().asSequence()
                        .filter { currentTimeMs > it.value }        // passed timeout
                        .map { it.key }                             // get guildId
                        .chunked(50)                                // chunk w/ maximum size per chunk of 50
                        .toCollection(LinkedList())

                    // push to a separate collection in order to evade
                    //concurrent modification of underlying set.
                    requests.forEach(this@GuildSetupManager::sendChunkRequest)
                }
            }
        }
    }

    override fun close() {
        if(::worker.isInitialized)
            worker.cancel()
        dispatcher.close()
    }

    companion object {
        private const val ChunkTimeout = 1000L

        val Log = createLogger(GuildSetupManager::class)
    }
}
