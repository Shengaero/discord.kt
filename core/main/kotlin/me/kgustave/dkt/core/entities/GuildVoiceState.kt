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
package me.kgustave.dkt.core.entities

import me.kgustave.dkt.core.DiscordBot

interface GuildVoiceState {
    // NOTE:
    //
    // We name this GuildVoiceState as opposed to VoiceState
    // for compatibility if BOT accounts are ever allowed in
    // group calls one day, in which case we will extend this
    // as a subtype of VoiceState.

    val bot: DiscordBot
    val guild: Guild
    val channel: VoiceChannel?
    val user: User
    val member: Member
    val sessionId: String?
    val deaf: Boolean
    val mute: Boolean
    val selfDeaf: Boolean
    val selfMute: Boolean
    val suppress: Boolean
}
