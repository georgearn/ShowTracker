package com.georgearn.showtracker.ui.screens.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The frosted back/share buttons over the hero must not leak taps into the page underneath. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailsTopBarHitTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun glassButtons_overImage_handleTapsThemselves() {
        var back = 0
        var share = 0
        var content = 0
        rule.setContent {
            MaterialTheme {
                val backdrop = rememberGlassBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().glassBackdrop(backdrop).clickable { content++ })
                    DetailsTopBar(title = null, overImage = true, onBack = { back++ }, onShare = { share++ }, backdrop = backdrop)
                }
            }
        }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithContentDescription("Share").performClick()
        rule.waitForIdle()
        assertEquals(1, back)
        assertEquals(1, share)
        assertEquals(0, content)
    }
}
