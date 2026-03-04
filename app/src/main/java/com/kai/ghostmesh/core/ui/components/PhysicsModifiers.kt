package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * High-Fidelity Tactile Feedback: "Fidget Physics" for MD3E.
 * - Pronounced scale-down (0.92f) for "Squishy" feel.
 * - High-damping (0.85f) springs for organic response.
 * - Dynamic corner sharpening handled in Composable layer.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.jellyClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val springSpec = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = 0.85f
    )

    // Deep Scale: Pronounced squish on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = springSpec,
        label = "tactile_scale"
    )

    // Interaction Pulse: Expands slightly on trigger
    val stretchX by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1f,
        animationSpec = springSpec,
        label = "tactile_stretch_x"
    )

    this
        .graphicsLayer {
            scaleX = scale * stretchX
            scaleY = scale
            translationY = if (isPressed) 3.dp.toPx() else 0f
            shadowElevation = if (isPressed) 0f else 4.dp.toPx()
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick?.invoke()
            }
        )
}

/**
 * Proximity Awareness: Simulates physical displacement for nearby elements.
 */
fun Modifier.proximityDisplacement(
    isTargetInteracted: Boolean
) = composed {
    val displacement by animateDpAsState(
        targetValue = if (isTargetInteracted) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f),
        label = "proximity"
    )

    this.graphicsLayer {
        translationY = displacement.toPx()
    }
}

fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f),
        label = "mag_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.physicalTilt(
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rotationX by animateFloatAsState(
        targetValue = if (isPressed) -8f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f),
        label = "tilt_x"
    )

    this.graphicsLayer {
        if (enabled) {
            this.rotationX = rotationX
            cameraDistance = 12f * density
        }
    }
}

const val StiffnessLow = 120f
const val StiffnessMediumLow = 400f
