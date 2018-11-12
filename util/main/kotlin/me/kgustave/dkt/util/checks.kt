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
@file:JvmName("ChecksUtil")
package me.kgustave.dkt.util

import java.util.concurrent.RejectedExecutionException
import kotlin.contracts.contract

// IllegalArgumentException

/**
 * Checks if a string is longer than the specified length,
 * outputting a helpful error if it is.
 *
 * If the string is `null` this does nothing.
 */
fun requireNotLonger(string: String?, length: Int, name: String) {
    string ?: return
    require(string.length <= length) { "$name cannot be longer than $length characters" }
}

/**
 * Throws an exception if the provided string is blank.
 *
 * This is not the same as the string being empty, as this checks
 * for whitespace as well.
 *
 * If the string is `null` this does nothing.
 */
fun requireNotBlank(string: String?, name: String) {
    string ?: return
    require(string.isNotBlank()) { "$name cannot be blank" }
}

// RejectedExecutionException

/**
 * Throws a [RejectedExecutionException] if [condition] is `true`.
 */
fun reject(condition: Boolean) {
    contract { returns() implies (!condition) }
    if(condition) throw RejectedExecutionException()
}

/**
 * Throws a [RejectedExecutionException] if [condition] is `true`.
 */
inline fun reject(condition: Boolean, msg: () -> String) {
    contract { returns() implies (!condition) }
    if(condition) throw RejectedExecutionException(msg())
}

// UnsupportedOperationException

/**
 * Throws a [UnsupportedOperationException].
 */
inline fun unsupported(msg: () -> String): Nothing {
    throw UnsupportedOperationException(msg())
}
