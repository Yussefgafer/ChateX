package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Fidget Physics: Magnetic Clickable
 * Implements squish (0.92f), magnetic release (overshoot), and dual-stage haptics.
 */
fun Modifier.magneticClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
) = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed) 0.4f else 0.35f,
            stiffness = if (isPressed) StiffnessMediumLow else StiffnessLow
        ),
        label = "magnetic_squish"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Tick
                        if (tryAwaitRelease()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Pop
                            onClick()
                        }
                    }
                )
            }
        }
}

/**
 * Fidget Physics: Physical Tilt
 * Implements a 3D leaning effect towards the touch point.
 */
fun Modifier.physicalTilt(
    enabled: Boolean = true
) = composed {
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val animatedTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = StiffnessLow),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = StiffnessLow),
        label = "tiltY"
    )

    this
        .graphicsLayer {
            rotationX = animatedTiltX
            rotationY = animatedTiltY
            cameraDistance = 12f * density
        }
        .pointerInput(enabled) {
            if (enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitFirstDown(requireUnconsumed = false)
                        val size = this.size

                        // Calculate tilt based on touch position relative to center
                        tiltY = (event.position.x - size.width / 2) / (size.width / 2) * 10f
                        tiltX = -(event.position.y - size.height / 2) / (size.height / 2) * 10f

                        waitForUpOrCancellation()
                        tiltX = 0f
                        tiltY = 0f
                    }
                }
            }
        }
}

/**
 * Fidget Physics: Sticky Motion
 * Uses low stiffness springs to make movement feel "heavy" and "sticky".
 */
fun Modifier.stickyMotion(
    targetOffset: IntOffset
) = composed {
    val animatedOffset by animateIntOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = StiffnessLow
        ),
        label = "sticky_motion"
    )

    this.offset { animatedOffset }
}

const val StiffnessLow = 100f
const val StiffnessMediumLow = 300f
