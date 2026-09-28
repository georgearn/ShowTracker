package com.georgearn.showtracker.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Colour roles for [Pill]: NEUTRAL for facts (scores), ACCENT for time-sensitive info
 * (countdowns, "3d ago", new seasons), OVERLAY for pills drawn on top of artwork.
 */
enum class PillTone { NEUTRAL, ACCENT, OVERLAY }

/** The single pill shape used for all small metadata tags across the app. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tone: PillTone = PillTone.NEUTRAL) {
    val (container, content) = when (tone) {
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PillTone.ACCENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PillTone.OVERLAY -> MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f) to Color.White
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
