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
package me.kgustave.dkt.internal.impl

import me.kgustave.dkt.DiscordBot
import me.kgustave.dkt.DiscordBotShard
import me.kgustave.dkt.internal.DktInternal

@UseExperimental(DktInternal::class)
internal class DiscordBotShardImpl internal constructor(config: DiscordBotShard.Config): DiscordBotShard, DiscordBot by DiscordBotImpl(config) {
    override val shardId = config.shardId
    override val shardTotal = config.shardTotal
}
