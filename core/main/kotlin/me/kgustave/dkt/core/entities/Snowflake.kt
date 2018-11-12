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
package me.kgustave.dkt.core.entities

import me.kgustave.dkt.core.internal.util.snowflakeTimeOf
import java.time.OffsetDateTime

/**
 * A common trait among a majority of Discord entities, this interface represents this most raw and
 * basic commonality.
 *
 * Discord uses [Twitter's snowflake format](https://github.com/twitter/snowflake/tree/snowflake-2010)
 * to guarantee unique 64-bit integer IDs for all entities on Discord (except some very rare and unique
 * scenarios in which child objects share their parent's ID).
 *
 * An example of snowflake nature is how two [User] instances will **never** have the same [ID][User.id].
 *
 * @author Kaidan Gustave
 */
interface Snowflake {
    /**
     * A unique 64-bit integer [Long] ID for a Discord entity.
     *
     * Generally speaking, most things on Discord (such as [Users][User],
     * [Guilds][Guild], etc) possess a Snowflake ID.
     */
    val id: Long

    /**
     * The creation time of this Snowflake entity.
     *
     * The value of a Snowflake [ID][id] is actually comprised of
     * a couple key factors, one being the time that it is generated.
     *
     * This uses a "reverse snowflake" algorithm to determine the
     * Greenwich Mean Time (GMT) instance this Snowflake entity was
     * created. Specification is more detailed in the actual algorithm
     * implementation [here][me.kgustave.dkt.util.snowflakeTimeOf].
     */
    val creationTime: OffsetDateTime get() = snowflakeTimeOf(id)

    /**
     * Companion for the [Snowflake] interface.
     *
     * Contains utilities related to snowflake implementations.
     */
    companion object {

        /**
         * Checks whether two [Snowflakes][Snowflake] are equal, first based
         * on referential equality, and then (if not already `true`) based on
         * the equality of both [IDs][Snowflake.id].
         *
         * Usage of this is typically seen in internal implementations of
         * the Snowflake interface, generally when overriding [equals][Any.equals].
         *
         * @param S The type of [Snowflake] to check equality for.
         *
         * @param s1 The first [Snowflake].
         * @param s2 The second [Snowflake].
         *
         * @return `true` if [s1] and [s2] are *referentially* equal, or if the
         *         two have equivalent [IDs][Snowflake.id].
         */
        fun <S: Snowflake> equals(s1: S, s2: S): Boolean = s1 === s2 || s1.id == s2.id

        /**
         * Converts the provided [Snowflake] to a string using the provided
         * [identifier], in the following format:
         * ```
         * <identifier>:<snowflake.id>
         * ```
         * This is commonly used when overriding the [toString][Any.toString]
         * method of many Snowflake implementations.
         *
         * @param identifier The identifier.
         * @param snowflake The [Snowflake].
         *
         * @return A formatted string.
         */
        fun toString(identifier: String, snowflake: Snowflake): String = "$identifier:${snowflake.id}"
    }
}
