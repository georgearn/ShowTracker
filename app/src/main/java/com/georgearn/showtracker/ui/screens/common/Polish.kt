package com.georgearn.showtracker.ui.screens.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Soft, slightly bouncy spring used for all small UI motion, instead of linear/ease curves. */
fun <T> softSpring() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/** Scales the element down a touch while pressed, like a physical button. Pass the same source as its clickable. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, softSpring(), label = "pressScale")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Gentle fade-up the first time an item appears. The "already shown" flag is saved per item,
 * so scrolling back to it in a lazy list doesn't replay the animation.
 */
fun Modifier.fadeUpOnAppear(): Modifier = composed {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            launch { progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)) }
            shown = true
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/**
 * "Double-bezel" container: a faint outer tray with a hairline edge holding an inner panel with
 * concentric corners and a soft top highlight, so cards read like a plate seated in a frame.
 */
@Composable
fun BezelCard(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 28.dp,
    gap: Dp = 6.dp,
    innerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val outer = RoundedCornerShape(outerRadius)
    val inner = RoundedCornerShape(outerRadius - gap)
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .clip(outer)
            .background(onSurface.copy(alpha = 0.04f))
            .border(1.dp, onSurface.copy(alpha = 0.08f), outer)
            .padding(gap)
    ) {
        Column(
            Modifier
                .clip(inner)
                .background(innerColor)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)),
                    inner
                ),
            content = content
        )
    }
}
