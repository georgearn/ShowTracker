package com.georgearn.showtracker.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class TestVm : ViewModel()

/** Renders the real TabbedNavDisplay (Navigation 3 + list-detail scene) with stand-in screens. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TabbedNavDisplayTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var nav: AppNavigator
    private val vms = mutableMapOf<String, TestVm>()

    private fun setUp() {
        rule.setContent {
            MaterialTheme {
                val stacks: Map<Route, MutableList<NavKey>> = remember {
                    TabRoutes.associateWith { SnapshotStateList<NavKey>().apply { add(it) } }
                }
                var tabIndex by remember { mutableIntStateOf(0) }
                nav = remember { AppNavigator(stacks, { TabRoutes[tabIndex] }, { tabIndex = TabRoutes.indexOf(it) }) }
                TabbedNavDisplay(
                    navigator = nav,
                    tabStacks = stacks,
                    providerFor = { tab ->
                        val ck: (Any) -> Any = { "$tab/$it" }
                        entryProvider {
                            entry<HomeRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.listPane()) { Screen("home", tab) }
                            entry<DiscoverRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.listPane()) { Screen("discover", tab) }
                            entry<WatchlistRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.listPane()) { Screen("watchlist", tab) }
                            entry<ForYouRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.listPane()) { Screen("foryou", tab) }
                            entry<DetailsRoute>(clazzContentKey = ck, metadata = ListDetailSceneStrategy.detailPane()) {
                                Screen("details-${it.tmdbId}", tab)
                            }
                        }
                    }
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Screen(name: String, tab: Route) {
        val vm: TestVm = viewModel()
        vms["$tab/$name"] = vm
        var count by rememberSaveable { mutableStateOf(0) }
        Column {
            Text("$name screen")
            Button(onClick = { count++ }) { Text("$name count $count") }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun compact_showsOneScreen_andTabsKeepStateAndViewModels() {
        setUp()
        rule.onNodeWithText("home screen").assertIsDisplayed()
        rule.runOnIdle { nav.selectTab(DiscoverRoute) }
        rule.onNodeWithText("discover count 0").performClick()
        rule.onNodeWithText("discover count 1").assertIsDisplayed()
        val discoverVm = vms.getValue("$DiscoverRoute/discover")

        rule.runOnIdle { nav.selectTab(WatchlistRoute) }
        rule.onNodeWithText("watchlist screen").assertIsDisplayed()
        rule.runOnIdle { nav.selectTab(DiscoverRoute) }
        // Saved UI state and the same ViewModel instance survive the tab switch.
        rule.onNodeWithText("discover count 1").assertIsDisplayed()
        assertSame(discoverVm, vms.getValue("$DiscoverRoute/discover"))
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun sameTitleOpenInTwoTabs_doesNotCrash_andHasSeparateViewModels() {
        setUp()
        rule.runOnIdle { nav.navigate(DetailsRoute(7, "movie")) }
        rule.onNodeWithText("details-7 screen").assertIsDisplayed()
        rule.runOnIdle { nav.selectTab(DiscoverRoute); nav.navigate(DetailsRoute(7, "movie")) }
        rule.onNodeWithText("details-7 screen").assertIsDisplayed()
        assertNotSame(vms.getValue("$HomeRoute/details-7"), vms.getValue("$DiscoverRoute/details-7"))
        rule.runOnIdle { nav.goBack() }
        rule.onNodeWithText("discover screen").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1200dp-h800dp")
    fun wideWindow_showsListAndDetailSideBySide() {
        setUp()
        rule.runOnIdle { nav.navigate(DetailsRoute(3, "tv")) }
        rule.onNodeWithText("home screen").assertIsDisplayed()
        rule.onNodeWithText("details-3 screen").assertIsDisplayed()
        rule.runOnIdle { assertEquals(listOf<NavKey>(HomeRoute, DetailsRoute(3, "tv")), nav.visibleStack) }
    }
}
