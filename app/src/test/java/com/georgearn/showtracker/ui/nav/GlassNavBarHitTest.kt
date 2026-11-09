package com.georgearn.showtracker.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Taps on the floating glass bar must reach the bar, never the screen content underneath it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GlassNavBarHitTest {

    @get:Rule val rule = createComposeRule()

    private val destinations = listOf(
        TopLevelDestination("a", "Alpha", Icons.Filled.Home, Icons.Filled.Home),
        TopLevelDestination("b", "Beta", Icons.Filled.Search, Icons.Filled.Search)
    )

    private var contentClicks = 0
    private val selected = mutableListOf<String>()

    private fun setUp() {
        rule.setContent {
            MaterialTheme {
                val backdrop = rememberGlassBackdrop()
                Box(Modifier.fillMaxSize()) {
                    // Stand-in for the NavHost: clickable everywhere, including under the bar.
                    Box(Modifier.fillMaxSize().glassBackdrop(backdrop).clickable { contentClicks++ })
                    GlassNavBar(
                        backdrop = backdrop,
                        destinations = destinations,
                        currentRoute = "a",
                        onSelect = { selected += it.route },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    @Test
    fun tapOnItem_selectsIt_andDoesNotReachContent() {
        setUp()
        rule.onNodeWithText("Beta").performClick()
        rule.waitForIdle()
        assertEquals(listOf("b"), selected)
        assertEquals(0, contentClicks)
    }

    @Test
    fun tapOnBarEdge_doesNotReachContent() {
        setUp()
        // Left rim of the capsule (inside the bar's 6dp edge padding, outside any item).
        rule.onRoot().performTouchInput {
            val barY = bottom - with(density) { (12.dp + 32.dp).toPx() }
            click(androidx.compose.ui.geometry.Offset(left + with(density) { 23.dp.toPx() }, barY))
        }
        rule.waitForIdle()
        assertEquals(0, contentClicks)
    }

    @Test
    fun tapAboveBar_stillReachesContent() {
        setUp()
        rule.onRoot().performTouchInput { click(center) }
        rule.waitForIdle()
        assertEquals(1, contentClicks)
        assertEquals(emptyList<String>(), selected)
    }
}
