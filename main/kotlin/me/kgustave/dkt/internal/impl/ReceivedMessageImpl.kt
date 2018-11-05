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
package me.kgustave.dkt.internal.impl

import io.ktor.client.call.receive
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.exceptions.RequestException
import me.kgustave.dkt.internal.DktInternal
import me.kgustave.dkt.promises.RestPromise
import me.kgustave.dkt.promises.restPromise
import me.kgustave.dkt.requests.Route

@DktInternal
open class ReceivedMessageImpl(
    final override val bot: DiscordBotImpl,
    final override val id: Long,
    final override val type: Message.Type,
    final override val channel: MessageChannel,
    final override val content: String,
    final override val author: User,
    final override val embeds: List<Embed>,
    final override val attachments: List<Message.Attachment>,
    final override val isWebhook: Boolean
): Message {
    final override val channelType: Channel.Type get() = channel.type
    final override val guild: Guild? get() = channel.guild
    final override val member: Member? get() = guild?.getMember(author)
    final override val mentionedUsers: List<User> by lazy { emptyList<User>() }
    final override val mentionedEmotes: List<Emote> by lazy { emptyList<Emote>() }
    final override val mentionedChannels: List<TextChannel> by lazy { emptyList<TextChannel>() }

    override fun mentions(mentionable: Mentionable): Boolean {
        return when(mentionable) {
            is User -> mentionable in mentionedUsers
            is Emote -> mentionable in mentionedEmotes
            is TextChannel -> mentionable in mentionedChannels
            is Member -> mentionable.user in mentionedUsers
            else -> false
        }
    }

    override fun delete(): RestPromise<Unit> {
        return bot.restPromise(Route.DeleteMessage.format(channel.id, id)) { call ->
            if(call.response.status.value != 204) {
                throw call.response.receive<RequestException>()
            }
        }
    }
}
