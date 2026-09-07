package com.arno.showtracker.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arno.showtracker.ui.screens.details.DetailsScreen
import com.arno.showtracker.ui.screens.discover.DiscoverScreen
import com.arno.showtracker.ui.screens.foryou.ForYouScreen
import com.arno.showtracker.ui.screens.home.HomeScreen
import com.arno.showtracker.ui.screens.notifications.NotificationsScreen
import com.arno.showtracker.ui.screens.settings.SettingsScreen
import com.arno.showtracker.ui.screens.watchlist.WatchlistScreen

private object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val WATCHLIST = "watchlist"
    const val FOR_YOU = "foryou"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val DETAILS = "details/{tmdbId}/{mediaType}"
    fun details(id: Int, type: String) = "details/$id/$type"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Default.Home),
    TopLevelDestination(Routes.DISCOVER, "Discover", Icons.Default.Explore),
    TopLevelDestination(Routes.WATCHLIST, "List", Icons.Default.Bookmarks),
    TopLevelDestination(Routes.FOR_YOU, "For You", Icons.Default.AutoAwesome)
)

@Composable
fun ShowTrackerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == null || topLevelDestinations.any { it.route == currentRoute }) {
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
            startDestination = Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
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
