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
package me.kgustave.dkt.internal.data.serializers

import java.time.format.DateTimeFormatter as DTF // I'm DTF ;3

annotation class SerialTimeFormat(val kind: SerialTimeFormat.Kind) {
    enum class Kind(val formatter: DTF) {
        ISO_OFFSET_DATE_TIME(DTF.ISO_OFFSET_DATE_TIME),
        RFC_1123_DATE_TIME(DTF.RFC_1123_DATE_TIME),
        BASIC_ISO_DATE(DTF.BASIC_ISO_DATE)
    }
}
