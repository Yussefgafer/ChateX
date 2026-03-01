package com.kai.ghostmesh.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * HapticButton: Rebuilt for MD3E "Physical Key" aesthetic.
 * Uses inner shadows (via DrawScope) and gradients to look like a physical object.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HapticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.primaryContainer
    val shadowColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
    val highlightColor = Color.White.copy(alpha = 0.1f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 12.dp,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
    )

    Box(
        modifier = modifier
            .physicalTilt(enabled)
            .magneticClickable(onClick, enabled)
            .clip(RoundedCornerShape(animatedCornerRadius))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(highlightColor, Color.Transparent, shadowColor)
                    )
                )
            }
            .background(surfaceColor)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun HapticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .physicalTilt(enabled)
            .magneticClickable(onClick, enabled),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
