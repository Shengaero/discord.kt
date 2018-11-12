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
@file:Suppress("unused", "ConvertSecondaryConstructorToPrimary")

package me.kgustave.dkt.core.entities

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializer
import me.kgustave.dkt.core.DiscordBot
import me.kgustave.dkt.core.promises.MessagePromise
import me.kgustave.dkt.core.promises.RestPromise

interface Message: Snowflake {
    companion object {
        /**
         * The maximum number of text characters that
         * can be sent in a single message.
         */
        const val MaxTextLength = 2000

        const val MaxFileSize = 8 shl 20

        const val MaxFileNumber = 10

        internal fun contentEmpty(text: CharSequence?): Boolean = text == null || text.isBlank()
    }

    val bot: DiscordBot
    val guild: Guild?
    val type: Type
    val channel: MessageChannel
    val channelType: Channel.Type
    val content: String
    val author: User
    val member: Member?
    val embeds: List<Embed>
    val reactions: List<Reaction>
    val attachments: List<Attachment>
    val mentionedUsers: List<User>
    val mentionedChannels: List<TextChannel>
    val isWebhook: Boolean

    fun edit(text: String): MessagePromise = TODO("implement")

    fun delete(): RestPromise<Unit>

    infix fun mentions(mentionable: Mentionable): Boolean

    @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
    @Deprecated("Emote deprecated, no longer supporting this property", level = DeprecationLevel.ERROR)
    val mentionedEmotes: List<Emote> get() = emptyList()

    data class Attachment internal constructor(
        val id: Long,
        val url: String?,
        val proxyUrl: String?,
        val filename: String,
        val size: Int,
        val height: Int,
        val width: Int
    )

    enum class Type {
        DEFAULT(0),
        CHANNEL_PINNED_ADD(6),
        GUILD_MEMBER_JOIN(7),
        UNKNOWN(-1);

        // workaround for serializer

        val type: Int
        constructor(type: Int) { this.type = type }

        @Serializer(forClass = Type::class)
        companion object {
            fun of(type: Int): Message.Type = values().firstOrNull { it.type == type } ?: UNKNOWN
            override fun deserialize(input: Decoder): Type = of(input.decodeInt())
            override fun serialize(output: Encoder, obj: Type) = output.encodeInt(obj.ordinal)
        }
    }
}
