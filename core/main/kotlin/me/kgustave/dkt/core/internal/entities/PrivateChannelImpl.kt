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
package me.kgustave.dkt.core.internal.entities

import me.kgustave.dkt.core.entities.Guild
import me.kgustave.dkt.core.entities.PrivateChannel
import me.kgustave.dkt.core.internal.DktInternal
import me.kgustave.dkt.core.promises.MessagePromise
import me.kgustave.dkt.util.delegates.weak

@DktInternal
class PrivateChannelImpl internal constructor(override val id: Long, user: UserImpl): PrivateChannel {
    override val recipient by weak(user)

    override val bot: DiscordBotImpl get() = recipient.bot
    override val guild: Guild? get() = null

    override var lastMessageId: Long? = null
    override var untracked: Boolean = user.untracked

    override fun send(text: String): MessagePromise = MessagePromise(bot, this, text)
}
