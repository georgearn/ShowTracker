package com.georgearn.showtracker.ui.nav

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every route must round-trip through saved state (rotation / process death). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackStackRestoreTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun allRoutes_surviveSaveAndRestore() {
        val tester = StateRestorationTester(rule)
        var stack: NavBackStack<NavKey>? = null
        tester.setContent {
            val s = rememberNavBackStack(HomeRoute)
            stack = s
            Text(s.joinToString("|"))
        }
        val all = listOf(DiscoverRoute, WatchlistRoute, ForYouRoute, SettingsRoute, NotificationsRoute, AiPicksRoute, DetailsRoute(42, "tv"))
        rule.runOnIdle { stack!!.addAll(all) }
        val expected = (listOf(HomeRoute) + all).joinToString("|")
        rule.onNodeWithText(expected).assertIsDisplayed()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText(expected).assertIsDisplayed()
    }
}
