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
package me.kgustave.dkt.core.internal.cache

import me.kgustave.dkt.core.entities.Member
import me.kgustave.dkt.core.entities.cache.MemberCache
import me.kgustave.dkt.core.internal.DktInternal

@DktInternal
class MemberCacheImpl: MemberCache, AbstractCacheImpl<Member>(Member::name) {
    override fun getByNickname(nickname: String, ignoreCase: Boolean): List<Member> {
        if(nickname.isBlank())
            return emptyList()
        val returns = arrayListOf<Member>()
        for(entity in this) {
            val entityNickname = entity.nickname ?: continue
            if(entityNickname.equals(nickname, ignoreCase)) {
                returns += entity
            }
        }
        return returns
    }

    override fun getByUsername(username: String, ignoreCase: Boolean): List<Member> {
        if(username.isBlank())
            return emptyList()
        val returns = arrayListOf<Member>()
        for(entity in this) {
            if(entity.user.name.equals(username, ignoreCase)) {
                returns += entity
            }
        }
        return returns
    }
}
