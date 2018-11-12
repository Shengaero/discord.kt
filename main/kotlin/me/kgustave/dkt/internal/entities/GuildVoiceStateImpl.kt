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
package me.kgustave.dkt.internal.entities

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.util.delegates.weak

@DktInternal
class GuildVoiceStateImpl internal constructor(guild: GuildImpl, member: MemberImpl): GuildVoiceState {
    override val guild by weak(guild)
    override val member by weak(member)

    override var channel: VoiceChannelImpl? = null
    override var sessionId: String? = null
    override var deaf: Boolean = false
    override var mute: Boolean = false
    override var selfDeaf: Boolean = false
    override var selfMute: Boolean = false
    override var suppress: Boolean = false

    override val bot: DiscordBotImpl get() = guild.bot
    override val user: UserImpl get() = member.user

}
