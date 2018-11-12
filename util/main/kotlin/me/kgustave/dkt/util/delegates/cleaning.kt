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

import kotlin.reflect.KProperty

open class CleaningRef<T: Any> internal constructor(private var value: T?) {
    open operator fun getValue(instance: Any?, property: KProperty<*>): T? {
        val value = this.value
        this.value = null
        return value
    }

    open operator fun setValue(instance: Any?, property: KProperty<*>, value: T?) {
        this.value = value
    }
}

private class SynchronizedCleaningRef<T: Any>(value: T?): CleaningRef<T>(value) {
    @Synchronized override operator fun getValue(instance: Any?, property: KProperty<*>): T? {
        return super.getValue(instance, property)
    }

    @Synchronized override operator fun setValue(instance: Any?, property: KProperty<*>, value: T?) {
        super.setValue(instance, property, value)
    }
}

/**
 * Creates a reference delegate that cleans the value stored after it is retrieved.
 *
 * This means that properties that delegate to this can automate nullification
 * of the value after it is consumed.
 *
 * @param T The type stored.
 *
 * @param initial The initial value to store. If not specified, this remains is unset.
 * @param threadSafe Whether or not accessing the returned reference delegate is synchronized.
 */
fun <T: Any> cleaningRef(initial: T? = null, threadSafe: Boolean = false): CleaningRef<T> =
    if(threadSafe) SynchronizedCleaningRef(initial) else CleaningRef(initial)
