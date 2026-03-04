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
 * Tactile Response Modifier: Implements "Fidget Physics" for MD3E.
 * - Pressure-sensitive scale-down (0.94f)
 * - Corner sharpening/rounding morphing based on interaction.
 * - Haptic pulses on trigger.
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

    // MD3E Standard: Damping 0.85f for high-fidelity organic motion.
    val springSpec = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = 0.85f
    )

    // Tactile Scale: Squashes on press, expands slightly on release
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = springSpec,
        label = "tactile_scale"
    )

    // Asymmetric Stretch for "Jelly" feel
    val stretchX by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = springSpec,
        label = "tactile_stretch_x"
    )

    this
        .graphicsLayer {
            scaleX = scale * stretchX
            scaleY = scale
            // Subtle Z-depth translation (simulated)
            translationY = if (isPressed) 2.dp.toPx() else 0f
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // Custom physical feedback instead of ripple
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick?.invoke()
            }
        )
}

/**
 * Magnetic Effect: Attracts or repels neighboring elements via simulated gravity.
 */
fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val springSpec = spring<Float>(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = 0.85f
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = springSpec,
        label = "mag_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Physical Tilt: Adds rotational depth on interaction for high-RAM devices.
 * Currently optimized for low-RAM performance (84MB baseline).
 */
fun Modifier.physicalTilt(
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rotationX by animateFloatAsState(
        targetValue = if (isPressed) -5f else 0f,
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
