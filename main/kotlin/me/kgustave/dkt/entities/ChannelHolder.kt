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
package me.kgustave.dkt.entities

/**
 * A channel holder entity, that can have child [text][TextChannel]
 * and [voice][VoiceChannel] channels.
 *
 * Instances of this are either a [Guild] or a [Category].
 */
interface ChannelHolder {
    val textChannels: List<TextChannel>
    val voiceChannels: List<VoiceChannel>
}
