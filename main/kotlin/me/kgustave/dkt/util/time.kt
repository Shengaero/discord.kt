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
@file:JvmName("TimeUtil")
package me.kgustave.dkt.util

import io.ktor.util.date.GMTDate
import io.ktor.util.date.toGMTDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar.getInstance as calendarInstanceOf
import java.util.TimeZone.getTimeZone as timeZoneOf

private const val DISCORD_EPOCH = 1420070400000L
private const val TIMESTAMP_OFFSET = 22

inline val currentTimeMs inline get() = System.currentTimeMillis()

val currentOffsetDateTime: OffsetDateTime get() = OffsetDateTime.now()

fun snowflakeTimeOf(id: Long): OffsetDateTime {
    val timestamp = (id ushr TIMESTAMP_OFFSET) + DISCORD_EPOCH
    val gmt = calendarInstanceOf(timeZoneOf("GMT"))
    gmt.timeInMillis = timestamp
    return OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
}

fun parseOffsetDateTime(time: String, format: DateTimeFormatter): OffsetDateTime {
    return OffsetDateTime.parse(time, format)
}

@Deprecated(
    message = "Discord.kt no longer supports usage of GMTDate! " +
              "Use OffsetDateTime and currentOffsetDateTime instead!",
    replaceWith = ReplaceWith(
        expression = "currentOffsetDateTime",
        imports = ["me.kgustave.dkt.util.currentOffsetDateTime"]
    )
)
val currentGMTDate: GMTDate get() = currentOffsetDateTime.toInstant().toGMTDate()

@Deprecated(
    message = "Discord.kt no longer supports usage of GMTDate! " +
              "Use OffsetDateTime and parseOffsetDateTime instead!",
    replaceWith = ReplaceWith(
        expression = "parseOffsetDateTime(time, format)",
        imports = ["me.kgustave.dkt.util.parseOffsetDateTime"]
    )
)
fun parseGMTDate(time: String, format: DateTimeFormatter): GMTDate {
    return parseOffsetDateTime(time, format).toInstant().toGMTDate()
}
