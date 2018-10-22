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
package me.kgustave.dkt.util.delegates

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class WeakDelegate<T> internal constructor(value: T) {

    // properties left internal for testing

    // whether this value is set as null
    //this is to allow the weak delegate to return a null value
    //for nullable entities but only when it's strictly set to null!
    internal var isSetToNull = value == null
    internal var ref = WeakReference<T>(value)

    operator fun getValue(instance: Any?, property: KProperty<*>): T {
        val value = ref.get()
        if(!isSetToNull) {
            check(value != null) { "Reference has already been garbage collected!" }
        }

        // cast to T instead of not-null assertion (!!),
        //this allows us to trick kotlin compiler into
        //returning a value that may or may not be null
        //even though T is "null-invariant".
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    operator fun setValue(instance: Any?, property: KProperty<*>, value: T) {
        ref.clear()
        ref = WeakReference(value)
        isSetToNull = value == null
    }
}

/**
 * Creates an [WeakDelegate] for the provided [value].
 *
 * This delegate is optionally mutable.
 *
 * @param value The initial value.
 *
 * @return An [WeakDelegate].
 */
fun <T> weak(value: T) = WeakDelegate(value)
