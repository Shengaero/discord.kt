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
package me.kgustave.dkt.rest

/**
 * Annotation for basic discord requester API.
 *
 * The discord REST API will be a standalone publicly exposed API in the
 * initial release of the full API, however at the moment, this is subject
 * to changes with little notice or deprecation.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Experimental(level = Experimental.Level.WARNING)
annotation class ExperimentalDktREST
