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

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

/** The current [system][System] time in milliseconds. */
inline val currentTimeMs: Long
    @JvmSynthetic inline get() = System.currentTimeMillis()

/** The current [OffsetDateTime]. */
inline val currentOffsetDateTime: OffsetDateTime
    // Interestingly enough, this is the most efficient form.
    //I personally would have thought the not-null assertion (IE: !!)
    //would "weight less" bytecode wise, but according to kotlinc,
    //allowing intrinsics to check it like this is just a tad smaller.
    @JvmSynthetic inline get() = OffsetDateTime.now()

/**
 * Parses the provided [time] string using the provided [formatter], returning
 * a resulting [OffsetDateTime].
 *
 * If no [formatter] is provided, this will default to using [ISO_OFFSET_DATE_TIME].
 *
 * This uses [OffsetDateTime.parse], more details on the exact functionality
 * of this function can be found there.
 *
 * @param time The time string to parse.
 * @param formatter The formatter to parse the time string with, default [ISO_OFFSET_DATE_TIME].
 *
 * @return An [OffsetDateTime] parsed from the [time] with the given [formatter].
 *
 * @see OffsetDateTime.parse
 */
fun parseOffsetDateTime(time: String, formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME): OffsetDateTime =
    OffsetDateTime.parse(time, formatter)
