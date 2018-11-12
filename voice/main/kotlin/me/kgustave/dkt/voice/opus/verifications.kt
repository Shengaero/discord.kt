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
@file:JvmName("OpusVerificationUtil")
package me.kgustave.dkt.voice.opus

import opus.ExperimentalOpus
import opus.Opus

private var init = false
private var audioIsSupported = false

internal fun isAudioSupported() = audioIsSupported
internal fun isAudioInitialized() = audioIsSupported && init

@UseExperimental(ExperimentalOpus::class)
@Synchronized internal fun loadOpus(): Boolean {
    if(init) return audioIsSupported

    init = true

    // TODO Handle exceptional result
    @Suppress("UNUSED_VARIABLE")
    val result = runCatching {
        audioIsSupported = Opus.isInitialized || Opus.initialize()
    }

    return audioIsSupported
}
