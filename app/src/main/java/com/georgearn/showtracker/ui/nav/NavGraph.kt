package com.georgearn.showtracker.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.georgearn.showtracker.ui.screens.aipicks.AiPicksScreen
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.LocalNavBarInset
import com.georgearn.showtracker.ui.screens.details.DetailsScreen
import com.georgearn.showtracker.ui.screens.discover.DiscoverScreen
import com.georgearn.showtracker.ui.screens.foryou.ForYouScreen
import com.georgearn.showtracker.ui.screens.home.HomeScreen
import com.georgearn.showtracker.ui.screens.notifications.NotificationsScreen
import com.georgearn.showtracker.ui.screens.onboarding.OnboardingScreen
import com.georgearn.showtracker.ui.screens.settings.SettingsScreen
import com.georgearn.showtracker.ui.screens.watchlist.WatchlistScreen
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

internal data class TopLevelDestination(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// M3 "fade through": outgoing fades fast, incoming fades + scales up slightly after a short delay.
private val fadeThroughIn = fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 210, delayMillis = 90))
private val fadeThroughOut = fadeOut(tween(durationMillis = 90))

data class DeepLinkTarget(val tmdbId: Int, val mediaType: String)

internal val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelDestination(DiscoverRoute, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    TopLevelDestination(WatchlistRoute, "List", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    TopLevelDestination(ForYouRoute, "For You", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    var onboarding by rememberSaveable { mutableStateOf(resolved) }

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

    // One saveable back stack per tab; the current tab is saved as its index in TabRoutes.
    val tabStacks: Map<Route, MutableList<NavKey>> = mapOf(
        HomeRoute to rememberNavBackStack(HomeRoute),
        DiscoverRoute to rememberNavBackStack(DiscoverRoute),
        WatchlistRoute to rememberNavBackStack(WatchlistRoute),
        ForYouRoute to rememberNavBackStack(ForYouRoute)
    )
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val navigator = remember(tabStacks) {
        AppNavigator(
            tabStacks = tabStacks,
            readCurrentTab = { TabRoutes[currentTabIndex] },
            writeCurrentTab = { currentTabIndex = TabRoutes.indexOf(it) }
        )
    }

    LaunchedEffect(deepLinkTarget) {
        if (deepLinkTarget != null) {
            navigator.navigate(DetailsRoute(deepLinkTarget.tmdbId, deepLinkTarget.mediaType))
            onDeepLinkConsumed()
        }
    }

    val visibleStack = navigator.visibleStack
    val showNavBar = !onboarding && visibleStack.last() in TabRoutes
    val backdrop = rememberGlassBackdrop()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val openDetail: (Int, String) -> Unit = { id, type -> navigator.navigate(DetailsRoute(id, type)) }

    Scaffold(
        snackbarHost = {
            // Keep snackbars above the floating bar.
            SnackbarHost(
                snackbarHostState,
                Modifier.padding(bottom = if (showNavBar) GlassNavBarHeight + GlassNavBarMargin else 0.dp)
            )
        }
    ) { padding ->
        // Details draws its backdrop under the status bar, so the top inset is applied per screen.
        val belowStatusBar = Modifier.padding(top = padding.calculateTopPadding())
        if (onboarding) {
            Box(belowStatusBar.padding(bottom = padding.calculateBottomPadding())) {
                OnboardingScreen(onDone = { onboarding = false })
            }
            return@Scaffold
        }

        // A list pane shows "pick a title" beside it on wide windows until Details is opened.
        val listPane = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                EmptyState(
                    icon = Icons.Outlined.Movie,
                    title = "Pick a title",
                    body = "Its details open here."
                )
            }
        )
        // Content keys are prefixed with the tab, so the same title open in two tabs is two entries.
        fun providerFor(tab: Route) = entryProvider<NavKey> {
            val ck: (Any) -> Any = { "$tab/$it" }
            entry<HomeRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) {
                    HomeScreen(
                        onOpenDetail = openDetail,
                        onOpenNotifications = { navigator.navigate(NotificationsRoute) },
                        onOpenSettings = { navigator.navigate(SettingsRoute) }
                    )
                }
            }
            entry<DiscoverRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) { DiscoverScreen(onOpenDetail = openDetail) }
            }
            entry<WatchlistRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) { WatchlistScreen(onOpenDetail = openDetail) }
            }
            entry<ForYouRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) {
                    ForYouScreen(onOpenDetail = openDetail, onOpenAiPicks = { navigator.navigate(AiPicksRoute) })
                }
            }
            entry<AiPicksRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) {
                    AiPicksScreen(
                        onBack = { navigator.goBack() },
                        onOpenSettings = { navigator.navigate(SettingsRoute) },
                        onOpenDetail = openDetail
                    )
                }
            }
            entry<NotificationsRoute>(clazzContentKey = ck, metadata = listPane) {
                Box(belowStatusBar) {
                    NotificationsScreen(onBack = { navigator.goBack() }, onOpenDetail = openDetail)
                }
            }
            entry<SettingsRoute>(clazzContentKey = ck) {
                Box(belowStatusBar) { SettingsScreen(onBack = { navigator.goBack() }) }
            }
            entry<DetailsRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.detailPane()) { key ->
                DetailsScreen(
                    tmdbId = key.tmdbId,
                    mediaType = key.mediaType,
                    onBack = { navigator.goBack() },
                    onOpenDetail = openDetail
                )
            }
        }

        CompositionLocalProvider(
            LocalGlassDarkTheme provides isDark,
            LocalNavBarInset provides if (showNavBar) GlassNavBarHeight + GlassNavBarMargin * 2 else 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                TabbedNavDisplay(
                    navigator = navigator,
                    tabStacks = tabStacks,
                    providerFor = ::providerFor,
                    // Content runs under the floating bar; the glass needs it there to frost.
                    modifier = Modifier
                        .glassBackdrop(backdrop)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = padding.calculateBottomPadding())
                )
                if (showNavBar) {
                    GlassNavBar(
                        backdrop = backdrop,
                        destinations = topLevelDestinations,
                        currentRoute = navigator.currentTab,
                        onSelect = { navigator.selectTab(it.route) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = padding.calculateBottomPadding())
                            .padding(horizontal = 20.dp, vertical = GlassNavBarMargin)
                    )
                }
            }
        }
    }
}

/**
 * NavDisplay over per-tab back stacks. Every tab's entries stay decorated (so they keep their
 * ViewModels and saved UI state) while another tab is shown; only Home's stack plus the current
 * tab's stack are handed to NavDisplay. Wide windows get a list-detail scene.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun TabbedNavDisplay(
    navigator: AppNavigator,
    tabStacks: Map<Route, MutableList<NavKey>>,
    providerFor: (Route) -> (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier
) {
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>()
    )
    val decoratedByTab: Map<Route, List<NavEntry<NavKey>>> = TabRoutes.associateWith { tab ->
        rememberDecoratedNavEntries(tabStacks.getValue(tab), decorators, remember(tab) { providerFor(tab) })
    }
    val current = navigator.currentTab
    val visibleEntries = decoratedByTab.getValue(HomeRoute) +
        if (current == HomeRoute) emptyList() else decoratedByTab.getValue(current)

    NavDisplay(
        entries = visibleEntries,
        onBack = { navigator.goBack() },
        sceneStrategies = listOf(rememberListDetailSceneStrategy()),
        transitionSpec = { fadeThroughIn togetherWith fadeThroughOut },
        popTransitionSpec = { fadeThroughIn togetherWith fadeThroughOut },
        predictivePopTransitionSpec = { fadeThroughIn togetherWith fadeThroughOut },
        modifier = modifier
    )
}
