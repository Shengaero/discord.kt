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
package me.kgustave.dkt.core.test.tags

import org.junit.jupiter.api.Tag
import java.lang.annotation.Inherited

/**
 * Tag for tests that are considered "slow".
 *
 * These tests generally take up time, and should
 * only be enabled for build functionality testing.
 */
@Inherited
@Tag("slow")
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Slow

/**
 * Tag for tests that use the discord API.
 *
 * These should be disabled for build functionality testing,
 * but enabled for non-build testing.
 */
@Inherited
@Tag("uses-api")
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class UsesAPI
