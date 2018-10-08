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
package me.kgustave.dkt.events

import io.ktor.util.date.GMTDate
import me.kgustave.dkt.DiscordBot

class ReadyEvent internal constructor(override val bot: DiscordBot): Event

class ShutdownEvent internal constructor(override val bot: DiscordBot, val time: GMTDate, val code: Int): Event

class ReconnectEvent internal constructor(override val bot: DiscordBot): Event

class ResumeEvent internal constructor(override val bot: DiscordBot): Event
