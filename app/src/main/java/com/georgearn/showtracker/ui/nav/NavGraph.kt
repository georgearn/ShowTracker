package com.georgearn.showtracker.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.georgearn.showtracker.ui.screens.details.DetailsScreen
import com.georgearn.showtracker.ui.screens.discover.DiscoverScreen
import com.georgearn.showtracker.ui.screens.foryou.ForYouScreen
import com.georgearn.showtracker.ui.screens.home.HomeScreen
import com.georgearn.showtracker.ui.screens.justdropped.JustDroppedScreen
import com.georgearn.showtracker.ui.screens.notifications.NotificationsScreen
import com.georgearn.showtracker.ui.screens.onboarding.OnboardingScreen
import com.georgearn.showtracker.ui.screens.settings.SettingsScreen
import com.georgearn.showtracker.ui.screens.upcoming.UpcomingScreen
import com.georgearn.showtracker.ui.screens.watchlist.WatchlistScreen

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val WATCHLIST = "watchlist"
    const val FOR_YOU = "foryou"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val UPCOMING = "upcoming"
    const val JUST_DROPPED = "just_dropped"
    const val DETAILS = "details/{tmdbId}/{mediaType}"
    fun details(id: Int, type: String) = "details/$id/$type"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

data class DeepLinkTarget(val tmdbId: Int, val mediaType: String)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Default.Home),
    TopLevelDestination(Routes.DISCOVER, "Discover", Icons.Default.Explore),
    TopLevelDestination(Routes.WATCHLIST, "List", Icons.Default.Bookmarks),
    TopLevelDestination(Routes.FOR_YOU, "For You", Icons.Default.AutoAwesome)
)

@Composable
fun ShowTrackerNavHost(
    deepLinkTarget: DeepLinkTarget? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val gateViewModel: OnboardingGateViewModel = hiltViewModel()
    val needsOnboarding by gateViewModel.needsOnboarding.collectAsStateWithLifecycle()

    val resolved = needsOnboarding
    if (resolved == null) {
        Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    androidx.compose.runtime.LaunchedEffect(deepLinkTarget) {
        if (deepLinkTarget != null) {
            navController.navigate(Routes.details(deepLinkTarget.tmdbId, deepLinkTarget.mediaType))
            onDeepLinkConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != null && topLevelDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (resolved) Routes.ONBOARDING else Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenUpcoming = { navController.navigate(Routes.UPCOMING) },
                    onOpenJustDropped = { navController.navigate(Routes.JUST_DROPPED) }
                )
            }
            composable(Routes.UPCOMING) {
                UpcomingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                )
            }
            composable(Routes.JUST_DROPPED) {
                JustDroppedScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                )
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
            }
            composable(Routes.WATCHLIST) {
                WatchlistScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
            }
            composable(Routes.FOR_YOU) {
                ForYouScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                )
            }
            composable(
                route = Routes.DETAILS,
                arguments = listOf(
                    navArgument("tmdbId") { type = androidx.navigation.NavType.IntType },
                    navArgument("mediaType") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                DetailsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
