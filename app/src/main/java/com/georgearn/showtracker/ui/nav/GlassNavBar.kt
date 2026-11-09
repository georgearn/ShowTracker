package com.georgearn.showtracker.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassEffectState

internal val GlassNavBarHeight = 64.dp
internal val GlassNavBarMargin = 12.dp

/**
 * Frosted glass: a much heavier blur than Vitro's default Regular (which stays see-through like
 * iOS), plus a light dimming layer so labels stay legible over busy posters.
 */
private val FrostedGlass = Glass.Regular.copy(blurRadius = 18.dp, saturation = 1.3f, dimming = 0.12f)

/** Floating capsule nav bar drawn in frosted glass over [backdrop]. */
@Composable
internal fun GlassNavBar(
    backdrop: GlassBackdrop,
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberGlassEffectState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassNavBarHeight)
            .glassEffect(backdrop, FrostedGlass, CircleShape, state = glassState)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEach { dest ->
            val selected = currentRoute == dest.route
            val contentColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else glassState.contentColor.copy(alpha = 0.75f),
                label = "navItemColor"
            )
            val indicator by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                label = "navIndicator"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp)
                    .clip(CircleShape)
                    .background(indicator)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(dest) }
                    )
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Text(dest.label, style = MaterialTheme.typography.labelSmall, color = contentColor)
                }
            }
        }
    }
}
