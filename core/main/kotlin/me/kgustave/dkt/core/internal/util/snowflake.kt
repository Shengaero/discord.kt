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
@file:JvmName("SnowflakeUtil__Internal")
package me.kgustave.dkt.core.internal.util

import kotlinx.serialization.json.*
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import java.util.Calendar.getInstance as calendarInstanceOf
import java.util.TimeZone.getTimeZone as timeZoneOf

// Snowflake Creation Time

private const val DISCORD_EPOCH = 1420070400000L
private const val TIMESTAMP_OFFSET = 22

fun snowflakeTimeOf(id: Long): OffsetDateTime {
    val timestamp = (id ushr TIMESTAMP_OFFSET) + DISCORD_EPOCH
    val gmt = calendarInstanceOf(timeZoneOf("GMT"))
    gmt.timeInMillis = timestamp
    return OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
}

// Snowflake Json Handling

val JsonElement.snowflake: Long get() {
    checkElementIsSnowflake(this, false)
    return checkNotNull(longOrNull) { "Not a snowflake element!" }
}

val JsonElement.snowflakeOrNull: Long? get() {
    checkElementIsSnowflake(this, true)
    return longOrNull
}

@ExperimentalContracts
private fun checkElementIsSnowflake(element: JsonElement, allowNull: Boolean = false) {
    contract { returns() implies (element is JsonPrimitive) }
    check(allowNull || element !is JsonNull) { "Null is not a snowflake element!" }
    check(element !is JsonObject) { "Object is not a snowflake element!" }
    check(element !is JsonArray) { "Array is not a snowflake element!" }
}
