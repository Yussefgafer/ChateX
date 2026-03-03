package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Fidget Physics: Magnetic Effect
 * Only handles the visual and haptic "squish" effect.
 * Does NOT handle the click action itself to avoid conflicts with Buttons/Sliders.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = StiffnessMediumLow),
        label = "magnetic_squish"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fidget Physics: Physical Tilt
 * Implements a subtle 3D leaning effect.
 */
fun Modifier.physicalTilt(
    enabled: Boolean = true
) = composed {
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val animatedTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = StiffnessLow),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = StiffnessLow),
        label = "tiltY"
    )

    this
        .graphicsLayer {
            rotationX = animatedTiltX
            rotationY = animatedTiltY
            cameraDistance = 8f * density
        }
        .pointerInput(enabled) {
            if (enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitFirstDown(requireUnconsumed = false)
                        val size = this.size

                        tiltY = (event.position.x - size.width / 2) / (size.width / 2) * 5f
                        tiltX = -(event.position.y - size.height / 2) / (size.height / 2) * 5f

                        waitForUpOrCancellation()
                        tiltX = 0f
                        tiltY = 0f
                    }
                }
            }
        }
}

const val StiffnessLow = 120f
const val StiffnessMediumLow = 400f
