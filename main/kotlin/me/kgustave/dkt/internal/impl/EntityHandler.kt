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
@file:Suppress("MemberVisibilityCanBePrivate", "FoldInitializerAndIfToElvis")
package me.kgustave.dkt.internal.impl

import me.kgustave.dkt.internal.data.RawSelfUser
import me.kgustave.dkt.internal.data.RawUser

internal class EntityHandler(private val bot: DiscordBotImpl) {
    fun handleSelfUser(raw: RawSelfUser): SelfUserImpl {
        return when {
            // already initialized self user
            bot.selfIsInit() -> bot.self.also {
                // patch in new data
                it.patch(raw)
            }

            // need to create and cache self user
            else -> SelfUserImpl(bot, raw).also {
                // initialize self
                bot.self = it
                // cache
                bot.userCache[it.id] = it
            }
        }
    }

    fun handleUser(raw: RawUser, modifyCache: Boolean = true): UserImpl {
        var impl = bot.userCache[raw.id]

        if(impl != null) impl.patch(raw) else {
            impl = UserImpl(bot, raw)
            if(modifyCache) {
                bot.userCache[impl.id] = impl
            }
        }

        return impl
    }
}
