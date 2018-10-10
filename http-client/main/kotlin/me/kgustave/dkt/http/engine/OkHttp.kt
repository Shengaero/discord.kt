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
package me.kgustave.dkt.http.engine

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object OkHttp: HttpClientEngineFactory<OkHttp.Config> {
    override fun create(block: OkHttp.Config.() -> Unit): HttpClientEngine =
        OkHttpEngine(OkHttp.Config().apply(block))

    class Config: HttpClientEngineConfig() {
        internal var config: OkHttpClient.Builder.() -> Unit = {}

        fun config(block: OkHttpClient.Builder.() -> Unit) {
            val oldConfig = config
            config = {
                oldConfig()
                block()
            }

        }

        fun addInterceptor(interceptor: Interceptor) {
            config {
                addInterceptor(interceptor)
            }
        }

        fun addNetworkInterceptor(interceptor: Interceptor) {
            config {
                addNetworkInterceptor(interceptor)
            }
        }
    }
}
