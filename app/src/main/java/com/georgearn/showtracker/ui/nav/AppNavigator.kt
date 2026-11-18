package com.georgearn.showtracker.ui.nav

import androidx.navigation3.runtime.NavKey

/**
 * One back stack per bottom-bar tab, like the old NavHost's saveState/restoreState: switching tabs
 * keeps each tab's pushed screens. The visible stack is Home's stack plus the current tab's stack,
 * so Back from another tab's root returns to Home, and Back on Home's root leaves the app.
 *
 * Plain Kotlin over [MutableList]s (the Compose host passes saveable NavBackStacks) so the
 * behaviour is unit-tested without Compose.
 */
class AppNavigator(
    private val tabStacks: Map<Route, MutableList<NavKey>>,
    private val readCurrentTab: () -> Route,
    private val writeCurrentTab: (Route) -> Unit
) {
    init {
        require(TabRoutes.all { it in tabStacks }) { "Every tab needs a back stack" }
    }

    val currentTab: Route get() = readCurrentTab()

    private fun stackOf(tab: Route): MutableList<NavKey> = tabStacks.getValue(tab)

    /** Back stack to display, oldest first. */
    val visibleStack: List<NavKey>
        get() = if (currentTab == HomeRoute) stackOf(HomeRoute) else stackOf(HomeRoute) + stackOf(currentTab)

    /**
     * Pushes [route] onto the current tab. If it is already in that tab's stack (a double tap, or
     * Details A -> B -> A), pops back to it instead: a stack never holds the same key twice.
     */
    fun navigate(route: Route) {
        val stack = stackOf(currentTab)
        val existing = stack.indexOf(route)
        if (existing >= 0) {
            while (stack.lastIndex > existing) stack.removeAt(stack.lastIndex)
        } else {
            stack.add(route)
        }
    }

    /** Switches tab; re-selecting the current tab pops it back to its root. */
    fun selectTab(tab: Route) {
        require(tab in TabRoutes) { "$tab is not a tab" }
        if (tab == currentTab) {
            val stack = stackOf(tab)
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        } else {
            writeCurrentTab(tab)
        }
    }

    /** Handles Back. Returns false when there is nothing left to pop (Home root). */
    fun goBack(): Boolean {
        val stack = stackOf(currentTab)
        return when {
            stack.size > 1 -> { stack.removeAt(stack.lastIndex); true }
            currentTab != HomeRoute -> { writeCurrentTab(HomeRoute); true }
            else -> false
        }
    }
}
