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

interface Presence {
    val status: OnlineStatus
    val afk: Boolean
    val activity: Activity?

    class Builder internal constructor(base: Presence = Default) {
        var status = base.status
        var afk = base.afk
        var activity = base.activity
    }

    companion object Default: Presence {
        override val status = OnlineStatus.ONLINE
        override val afk = false
        override val activity = null as Activity?
    }
}
