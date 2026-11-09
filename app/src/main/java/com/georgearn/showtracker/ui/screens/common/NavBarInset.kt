package com.georgearn.showtracker.ui.screens.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Space the floating glass nav bar covers at the bottom of top-level screens. Scrolling content
 * adds it to its bottom padding so the last item can scroll clear of the bar. 0 elsewhere.
 */
val LocalNavBarInset = staticCompositionLocalOf { 0.dp }
