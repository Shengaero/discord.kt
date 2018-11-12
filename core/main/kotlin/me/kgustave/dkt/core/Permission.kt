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
package me.kgustave.dkt.core

enum class Permission(
    val offset: Int,
    val isGuild: Boolean,
    val isChannel: Boolean,
    val permissionName: String
) {
    CREATE_INSTANT_INVITE(0, true, true, "Create Instant Invite"),
    KICK_MEMBERS(1, true, false, "Kick Members"),
    BAN_MEMBERS(2, true, false, "Ban Members"),
    ADMINISTRATOR(3, true, false, "Administrator"),
    MANAGE_CHANNEL(4, true, true, "Manage Channels"),
    MANAGE_SERVER(5, true, false, "Manage Server"),
    MESSAGE_ADD_REACTION(6, true, true, "Add Reactions"),
    VIEW_AUDIT_LOGS(7, true, false, "View Audit Logs"),
    PRIORITY_SPEAKER(8, true, true, "Priority Speaker"),

    // All Channels
    VIEW_CHANNEL(10, true, true, "Read Text Channels & See Voice Channels"),

    // Text Channels
    MESSAGE_READ(10, true, true, "Read Messages"),
    MESSAGE_WRITE(11, true, true, "Send Messages"),
    MESSAGE_TTS(12, true, true, "Send TTS Messages"),
    MESSAGE_MANAGE(13, true, true, "Manage Messages"),
    MESSAGE_EMBED_LINKS(14, true, true, "Embed Links"),
    MESSAGE_ATTACH_FILES(15, true, true, "Attach Files"),
    MESSAGE_HISTORY(16, true, true, "Read History"),
    MESSAGE_MENTION_EVERYONE(17, true, true, "Mention Everyone"),
    MESSAGE_EXT_EMOJI(18, true, true, "Use External Emojis"),

    // Voice Channels
    VOICE_CONNECT(20, true, true, "Connect"),
    VOICE_SPEAK(21, true, true, "Speak"),
    VOICE_MUTE_OTHERS(22, true, true, "Mute Members"),
    VOICE_DEAF_OTHERS(23, true, true, "Deafen Members"),
    VOICE_MOVE_OTHERS(24, true, true, "Move Members"),
    VOICE_USE_VAD(25, true, true, "Use Voice Activity"),

    NICKNAME_CHANGE(26, true, false, "Change Nickname"),
    NICKNAME_MANAGE(27, true, false, "Manage Nicknames"),

    MANAGE_ROLES(28, true, false, "Manage Roles"),
    MANAGE_PERMISSIONS(28, false, true, "Manage Permissions"),
    MANAGE_WEBHOOKS(29, true, true, "Manage Webhooks"),
    MANAGE_EMOTES(30, true, false, "Manage Emojis"),

    UNKNOWN(-1, false, false, "Unknown");

    val raw = 1L shl offset

    @ExperimentalUnsignedTypes
    @Deprecated(
        "Unsigned offset is not necessary given the low maximum offset of this enum. " +
        "This will be removed in a future release."
    )
    val uOffset = offset.toUInt()

    @ExperimentalUnsignedTypes val uRaw = 1UL shl offset

    val isText: Boolean get() = offset in TextRange
    val isVoice: Boolean get() = offset == 8 || offset == 10 || offset in VoiceRange

    companion object {
        @JvmField val TextRange = 10 until 20
        @JvmField val VoiceRange = 20 until 26

        @JvmField val All = values().toList()
        @JvmField val AllRaw =
            rawOf(All)
        @JvmField val AllGuild = All.filter(Permission::isGuild)
        @JvmField val AllGuildRaw =
            rawOf(AllGuild)
        @JvmField val AllChannel = All.filter(Permission::isChannel)
        @JvmField val AllChannelRaw =
            rawOf(AllChannel)
        @JvmField val AllText = All.filter(Permission::isText)
        @JvmField val AllTextRaw =
            rawOf(AllText)
        @JvmField val AllVoice = All.filter(Permission::isVoice)
        @JvmField val AllVoiceRaw =
            rawOf(AllVoice)

        @ExperimentalUnsignedTypes val AllURaw =
            unsignedRawOf(All)
        @ExperimentalUnsignedTypes val AllGuildURaw =
            unsignedRawOf(AllGuild)
        @ExperimentalUnsignedTypes val AllChannelURaw =
            unsignedRawOf(AllChannel)
        @ExperimentalUnsignedTypes val AllTextURaw =
            unsignedRawOf(AllText)
        @ExperimentalUnsignedTypes val AllVoiceURaw =
            unsignedRawOf(AllVoice)

        @JvmStatic
        fun setOf(raw: Long): Set<Permission> {
            if(raw == 0L) return emptySet()

            return values().asSequence()
                .filter { perm -> perm != UNKNOWN && (raw and perm.raw) == perm.raw }
                .toSet()
        }

        @JvmStatic
        fun fromOffset(offset: Int): Permission = values().firstOrNull { it.offset == offset } ?: UNKNOWN

        @JvmStatic
        fun rawOf(permissions: Collection<Permission>): Long =
            rawOf(*permissions.toTypedArray())

        @JvmStatic
        fun rawOf(vararg permissions: Permission): Long =
            permissions.asSequence()
                .filter { it != UNKNOWN }
                .fold(0L) { raw, permission -> raw or permission.raw }

        @JvmStatic
        @ExperimentalUnsignedTypes
        fun setOf(raw: ULong): Set<Permission> {
            if(raw == 0UL) return emptySet()

            return values().asSequence()
                .filter { perm -> perm != UNKNOWN && (raw and perm.uRaw) == perm.uRaw }
                .toSet()
        }

        @JvmStatic
        @Deprecated(
            "Unsigned offset is not necessary given the low maximum offset of this enum. " +
            "This will be removed in a future release."
        )
        @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
        fun fromOffset(offset: UInt): Permission = values().firstOrNull { it.uOffset == offset } ?: UNKNOWN

        @JvmStatic
        @ExperimentalUnsignedTypes
        fun unsignedRawOf(permissions: Collection<Permission>): ULong =
            unsignedRawOf(*permissions.toTypedArray())

        @JvmStatic
        @ExperimentalUnsignedTypes
        fun unsignedRawOf(vararg permissions: Permission): ULong =
            permissions.asSequence()
                .filter { it != UNKNOWN }
                .fold(0UL) { raw, permission -> raw or permission.uRaw }
    }
}
