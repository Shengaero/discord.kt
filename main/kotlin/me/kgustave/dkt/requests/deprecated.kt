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
@file:Suppress("unused")
package me.kgustave.dkt.requests

import me.kgustave.dkt.rest.BasicDiscordResponse
import me.kgustave.dkt.rest.BasicRoute
import me.kgustave.dkt.rest.DeleteMessageRoute
import me.kgustave.dkt.rest.DiscordCall
import me.kgustave.dkt.rest.DiscordRequest
import me.kgustave.dkt.rest.DiscordRequester
import me.kgustave.dkt.rest.DiscordResponse
import me.kgustave.dkt.rest.ErrorDiscordResponse
import me.kgustave.dkt.rest.ExperimentalDktREST
import me.kgustave.dkt.rest.FailedDiscordResponse
import me.kgustave.dkt.rest.FakeRoute
import me.kgustave.dkt.rest.FormattedRoute
import me.kgustave.dkt.rest.GlobalRateLimitProvider
import me.kgustave.dkt.rest.RateLimitedDiscordResponse
import me.kgustave.dkt.rest.Route

private const val NewPackage = "me.kgustave.dkt.rest"

@ExperimentalDktREST
@Deprecated("Renamed to ExperimentalDktREST and moved to $NewPackage. " +
            "This package will be removed by the initial stable release of the library!",
    ReplaceWith("ExperimentalDktREST", imports = ["$NewPackage.ExperimentalDktREST"]))
annotation class ExperimentalDiscordRequester

@Deprecated("Renamed to DiscordRequester and moved to $NewPackage. " +
            "This package will be removed by the initial stable release of the library!",
    ReplaceWith("DiscordRequester", imports = ["$NewPackage.DiscordRequester"]))
typealias Requester = DiscordRequester

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("GlobalRateLimitProvider", imports = ["$NewPackage.GlobalRateLimitProvider"]))
typealias GlobalRateLimitProvider = GlobalRateLimitProvider

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("DiscordCall", imports = ["$NewPackage.DiscordCall"]))
typealias DiscordCall = DiscordCall

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("DiscordRequest", imports = ["$NewPackage.DiscordRequest"]))
typealias DiscordRequest = DiscordRequest

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("DiscordResponse", imports = ["$NewPackage.DiscordResponse"]))
typealias DiscordResponse = DiscordResponse

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("BasicDiscordResponse", imports = ["$NewPackage.BasicDiscordResponse"]))
typealias BasicDiscordResponse = BasicDiscordResponse

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("FailedDiscordResponse", imports = ["$NewPackage.FailedDiscordResponse"]))
typealias FailedDiscordResponse = FailedDiscordResponse

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("ErrorDiscordResponse", imports = ["$NewPackage.ErrorDiscordResponse"]))
typealias ErrorDiscordResponse = ErrorDiscordResponse

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("RateLimitedDiscordResponse", imports = ["$NewPackage.RateLimitedDiscordResponse"]))
typealias RateLimitedDiscordResponse = RateLimitedDiscordResponse

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("Route", imports = ["$NewPackage.Route"]))
typealias Route = Route

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("BasicRoute", imports = ["$NewPackage.BasicRoute"]))
typealias BasicRoute = BasicRoute

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("FormattedRoute", imports = ["$NewPackage.FormattedRoute"]))
typealias FormattedRoute = FormattedRoute

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("DeleteMessageRoute", imports = ["$NewPackage.DeleteMessageRoute"]))
typealias DeleteMessageRoute = DeleteMessageRoute

@Deprecated("Moved to $NewPackage. This package will be " +
            "removed by the initial stable release of the library!",
    ReplaceWith("FakeRoute", imports = ["$NewPackage.FakeRoute"]))
typealias FakeRoute = FakeRoute
