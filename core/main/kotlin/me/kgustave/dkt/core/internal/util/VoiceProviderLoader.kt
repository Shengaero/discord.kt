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
package me.kgustave.dkt.core.internal.util

import me.kgustave.dkt.core.internal.entities.GuildImpl
import me.kgustave.dkt.core.managers.VoiceManager
import me.kgustave.dkt.core.voice.ExperimentalVoiceAPI

@ExperimentalVoiceAPI
internal object VoiceProviderLoader {
    private const val TargetFQN = "me.kgustave.dkt.voice.VoiceManagerProvideImpl"

    @Suppress("ObjectPropertyName")
    private var _provider: VoiceManager.Provider = Default
    private var hasInitialized = false

    @get:Synchronized internal val provider: VoiceManager.Provider get() {
        if(hasInitialized) {
            return _provider
        }

        hasInitialized = true

        val clazz = runCatching { Class.forName(TargetFQN) }.getOrNull() ?: return _provider
        val constructor = clazz.constructors.find { it.parameters.isEmpty() } ?: return _provider
        if(VoiceManager.Provider::class.java.isAssignableFrom(clazz)) {
            (constructor.newInstance() as? VoiceManager.Provider)?.let { _provider = it }
        }

        return _provider
    }

    private object Default: VoiceManager.Provider {
        override fun provide(guild: GuildImpl): VoiceManager {
            // FIXME More detailed error!
            throw UnsupportedOperationException("Voice not supported!")
        }
    }
}
