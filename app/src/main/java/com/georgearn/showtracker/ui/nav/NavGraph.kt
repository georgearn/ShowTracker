package com.georgearn.showtracker.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// M3 "fade through": outgoing fades fast, incoming fades + scales up slightly after a short delay.
private val fadeThroughIn = fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 210, delayMillis = 90))
private val fadeThroughOut = fadeOut(tween(durationMillis = 90))

data class DeepLinkTarget(val tmdbId: Int, val mediaType: String)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelDestination(Routes.DISCOVER, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    TopLevelDestination(Routes.WATCHLIST, "List", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    TopLevelDestination(Routes.FOR_YOU, "For You", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            gateViewModel.messages.collect { message ->
                val result = snackbarHostState.showSnackbar(
                    message = message.text,
                    actionLabel = message.actionLabel,
                    withDismissAction = message.actionLabel == null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) gateViewModel.runAction(message)
            }
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(deepLinkTarget) {
        if (deepLinkTarget != null) {
            navController.navigate(Routes.details(deepLinkTarget.tmdbId, deepLinkTarget.mediaType))
            onDeepLinkConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute != null && topLevelDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Details draws its backdrop under the status bar, so the top inset is applied per screen.
        val belowStatusBar = Modifier.padding(top = padding.calculateTopPadding())
        NavHost(
            navController = navController,
            startDestination = if (resolved) Routes.ONBOARDING else Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeThroughIn },
            exitTransition = { fadeThroughOut },
            popEnterTransition = { fadeThroughIn },
            popExitTransition = { fadeThroughOut }
        ) {
            composable(Routes.ONBOARDING) {
                Box(belowStatusBar) {
                    OnboardingScreen(
                        onDone = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
            }
            composable(Routes.HOME) {
                Box(belowStatusBar) {
                    HomeScreen(
                        onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) },
                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenUpcoming = { navController.navigate(Routes.UPCOMING) },
                        onOpenJustDropped = { navController.navigate(Routes.JUST_DROPPED) }
                    )
                }
            }
            composable(Routes.UPCOMING) {
                Box(belowStatusBar) {
                    UpcomingScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                    )
                }
            }
            composable(Routes.JUST_DROPPED) {
                Box(belowStatusBar) {
                    JustDroppedScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                    )
                }
            }
            composable(Routes.DISCOVER) {
                Box(belowStatusBar) {
                    DiscoverScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
                }
            }
            composable(Routes.WATCHLIST) {
                Box(belowStatusBar) {
                    WatchlistScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
                }
            }
            composable(Routes.FOR_YOU) {
                Box(belowStatusBar) {
                    ForYouScreen(onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) })
                }
            }
            composable(Routes.SETTINGS) {
                Box(belowStatusBar) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Routes.NOTIFICATIONS) {
                Box(belowStatusBar) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                    )
                }
            }
            composable(
                route = Routes.DETAILS,
                arguments = listOf(
                    navArgument("tmdbId") { type = androidx.navigation.NavType.IntType },
                    navArgument("mediaType") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                DetailsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { id, type -> navController.navigate(Routes.details(id, type)) }
                )
            }
        }
    }
}
