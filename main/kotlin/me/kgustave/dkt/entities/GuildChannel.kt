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

interface GuildChannel: Channel {
    val name: String
    val overrides: List<PermissionOverride>
    val position: Int
    val rawPosition: Int
    val parent: Category?

    override val guild: Guild // override guild for documentation changes and non-nullability

    fun permissionOverrideFor(member: Member): PermissionOverride?
    fun permissionOverrideFor(role: Role): PermissionOverride?
}
