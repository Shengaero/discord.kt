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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package me.kgustave.dkt.entities

import me.kgustave.dkt.util.requireNotLonger
import java.awt.Color
import java.time.*

data class Embed internal constructor(
    val title: String?,
    val url: String?,
    val author: Embed.Author?,
    val description: String?,
    val image: Embed.Image?,
    val thumbnail: Embed.Thumbnail?,
    val fields: List<Embed.Field>,
    val timestamp: OffsetDateTime?,
    val footer: Embed.Footer?,
    val color: Color?,

    val type: Embed.Type = Type.RICH,
    val video: Embed.Video? = null,
    val provider: Embed.Provider? = null
) {
    companion object {
        /**
         * The max character length a [embed title][Embed.Title.text]
         * can contain.
         *
         * Currently this is 256 characters.
         */
        const val MaxTitleLength = 256

        /**
         * The max character length a [embed description][Embed.description]
         * or [embed footer][Embed.Footer.text] can contain.
         *
         * Currently this is 2048 characters.
         */
        const val MaxTextLength = 2048

        /**
         * The max character length any URL in an [Embed] can be.
         *
         * Currently this is 2000 characters.
         */
        const val MaxUrlLength = 2000

        /**
         * The cumulative max character length of all text areas that an [Embed] can contain.
         *
         * Currently this is 6000 characters.
         */
        const val MaxTotalLength = 6000

        /**
         * The max character length that a [embed field][Embed.Field.value] can contain.
         *
         * Currently this is 1024 characters.
         */
        const val MaxFieldValueLength = 1024

        /**
         * "Zero Width Space" character that can make some embed
         * text areas appear empty in the discord interface.
         */
        const val ZWSP = "\u200E"

        private val UrlRegex = Regex("\\s*(https?|attachment)://.+\\..{2,}\\s*", RegexOption.IGNORE_CASE)
        private val RGBRange get() = 0..255

        private fun String.assureNotEmpty(): String = if(isBlank()) ZWSP else this
        private fun checkUrl(url: String?) {
            if(url === null) return

            requireNotLonger(url, MaxUrlLength, "URL")
            require(url matches UrlRegex) { "URL is not a valid embeddable URL" }
        }
    }

    fun isEmpty(): Boolean {
        return fields.isEmpty() &&
               author === null &&
               title === null &&
               description === null &&
               thumbnail === null &&
               image === null
    }

    data class Title internal constructor(
        val text: String,
        val url: String? = null
    )

    data class Field internal constructor(
        val name: String,
        val value: String,
        val inline: Boolean
    )

    data class Author internal constructor(
        val name: String,
        val url: String?,
        val iconUrl: String?,
        val proxyUrl: String? = null,
        val proxyIconUrl: String? = null
    )

    data class Footer internal constructor(
        val text: String,
        val iconUrl: String? = null,
        val proxyIconUrl: String? = null
    )

    // As upsetting as this is, there is no guarantee that
    // an Discord Embed Image will remain the same structure
    // as a Discord Embed Thumbnail. For the sake of compatibility
    // if one ever changes, I created two identical classes....
    // -_-

    data class Image internal constructor(
        val url: String,
        val proxyUrl: String? = null,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Thumbnail internal constructor(
        val url: String,
        val proxyUrl: String? = null,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Video internal constructor(
        val url: String,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Provider internal constructor(
        val name: String?,
        val url: String?
    )

    enum class Type constructor(type: String? = null) {
        IMAGE,
        VIDEO,
        LINK,
        RICH,
        UNKNOWN("");

        val type: String = type ?: name.toLowerCase()
    }
}
