package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Modifier.magneticClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
) = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (isPressed) {
            MaterialTheme.motionScheme.fastSpatialSpec()
        } else {
            spring(
                dampingRatio = 0.4f, // Extra bouncy release
                stiffness = 400f
            )
        },
        label = "magnetic_squish"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            // Add subtle elevation shadow on press
            shadowElevation = if (isPressed) 2f else 0f
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
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = StiffnessMediumLow
        ),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = StiffnessMediumLow
        ),
        label = "tiltY"
    )

    this
        .graphicsLayer {
            rotationX = animatedTiltX
            rotationY = animatedTiltY
            cameraDistance = 16f * density // Higher camera for more natural tilt
            // Add slight scale on tilt for depth
            val tiltMagnitude = Math.abs(animatedTiltX) + Math.abs(animatedTiltY)
            scaleX = 1f + (tiltMagnitude / 200f)
            scaleY = 1f + (tiltMagnitude / 200f)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberMorphProgress(isToggled: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (isToggled) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "morph_progress"
    ).value
}
