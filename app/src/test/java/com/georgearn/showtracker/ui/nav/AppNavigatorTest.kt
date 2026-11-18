package com.georgearn.showtracker.ui.nav

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppNavigatorTest {

    private lateinit var stacks: Map<Route, MutableList<NavKey>>
    private var tab: Route = HomeRoute
    private lateinit var nav: AppNavigator

    @Before
    fun setUp() {
        stacks = TabRoutes.associateWith { mutableListOf<NavKey>(it) }
        tab = HomeRoute
        nav = AppNavigator(stacks, { tab }, { tab = it })
    }

    private val a = DetailsRoute(1, "movie")
    private val b = DetailsRoute(2, "tv")

    @Test
    fun homeRoot_backReturnsFalse_soTheAppCanExit() {
        assertFalse(nav.goBack())
        assertEquals(listOf<NavKey>(HomeRoute), nav.visibleStack)
    }

    @Test
    fun otherTab_isShownOnTopOfHome_andBackFromItsRootGoesHome() {
        nav.selectTab(DiscoverRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DiscoverRoute), nav.visibleStack)
        assertTrue(nav.goBack())
        assertEquals(HomeRoute, nav.currentTab)
        assertEquals(listOf<NavKey>(HomeRoute), nav.visibleStack)
    }

    @Test
    fun switchingTabs_keepsEachTabsPushedScreens() {
        nav.selectTab(DiscoverRoute)
        nav.navigate(a)
        nav.selectTab(WatchlistRoute)
        nav.navigate(b)
        nav.selectTab(DiscoverRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DiscoverRoute, a), nav.visibleStack)
        nav.selectTab(WatchlistRoute)
        assertEquals(listOf<NavKey>(HomeRoute, WatchlistRoute, b), nav.visibleStack)
    }

    @Test
    fun reselectingCurrentTab_popsToItsRoot() {
        nav.selectTab(DiscoverRoute)
        nav.navigate(a)
        nav.navigate(b)
        nav.selectTab(DiscoverRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DiscoverRoute), nav.visibleStack)
    }

    @Test
    fun navigatingToAKeyAlreadyInTheStack_popsBackToIt_neverDuplicates() {
        nav.navigate(a)
        nav.navigate(a) // double tap
        assertEquals(listOf<NavKey>(HomeRoute, a), nav.visibleStack)
        nav.navigate(b)
        nav.navigate(a) // A -> B -> A
        assertEquals(listOf<NavKey>(HomeRoute, a), nav.visibleStack)
    }

    @Test
    fun backPopsWithinTheCurrentTabFirst() {
        nav.selectTab(ForYouRoute)
        nav.navigate(AiPicksRoute)
        nav.navigate(a)
        assertTrue(nav.goBack())
        assertEquals(listOf<NavKey>(HomeRoute, ForYouRoute, AiPicksRoute), nav.visibleStack)
        assertTrue(nav.goBack())
        assertTrue(nav.goBack())
        assertEquals(HomeRoute, nav.currentTab)
        assertFalse(nav.goBack())
    }

    @Test(expected = IllegalArgumentException::class)
    fun selectTab_rejectsNonTabs() {
        nav.selectTab(SettingsRoute)
    }
}
