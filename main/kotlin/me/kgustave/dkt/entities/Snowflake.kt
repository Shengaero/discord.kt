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

import io.ktor.util.date.GMTDate
import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.util.snowflakeTimeOf

interface Snowflake {
    /** The [DiscordBot] that this [Snowflake] belongs to. */
    val bot: DiscordBot
    val id: Long
    val creationTime: GMTDate get() = snowflakeTimeOf(id)
}
