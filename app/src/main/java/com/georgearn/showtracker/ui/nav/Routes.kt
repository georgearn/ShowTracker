package com.georgearn.showtracker.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every destination in the app. Serializable so back stacks survive process death. */
@Serializable
sealed interface Route : NavKey

@Serializable data object HomeRoute : Route
@Serializable data object DiscoverRoute : Route
@Serializable data object WatchlistRoute : Route
@Serializable data object ForYouRoute : Route
@Serializable data object SettingsRoute : Route
@Serializable data object NotificationsRoute : Route
@Serializable data object AiPicksRoute : Route
@Serializable data class DetailsRoute(val tmdbId: Int, val mediaType: String) : Route

/** The four bottom-bar tabs, in bar order. Home is the start tab. */
internal val TabRoutes: List<Route> = listOf(HomeRoute, DiscoverRoute, WatchlistRoute, ForYouRoute)
