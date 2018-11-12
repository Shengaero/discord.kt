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
@file:JvmName("MiscUtil")
package me.kgustave.dkt.util

/**
 * Unsafely casts the receiver as it's own type.
 *
 * This is a centralized function that tricks the
 * compiler into casting a variable as itself, returning
 * the casted variable.
 *
 * This requires that [T] is **not explicitly specified**,
 * as the type should be inferred from the value provided.
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> unsafeCast(value: T?): T = value as T
