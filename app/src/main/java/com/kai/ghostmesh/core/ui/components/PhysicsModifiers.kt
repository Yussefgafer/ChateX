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
import com.kai.ghostmesh.core.ui.theme.GhostMotion

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.jellyClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = GhostMotion.TactileSpring,
        label = "scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
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

fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = GhostMotion.TactileSpring,
        label = "mag_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.proximityDisplacement(
    isNeighborInteracted: Boolean
) = composed {
    val translationY by animateDpAsState(
        targetValue = if (isNeighborInteracted) 12.dp else 0.dp,
        animationSpec = GhostMotion.MassSpringDp,
        label = "displacement"
    )

    this.graphicsLayer {
        this.translationY = translationY.toPx()
    }
}

fun Modifier.physicalTilt(
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rotationX by animateFloatAsState(
        targetValue = if (isPressed) -8f else 0f,
        animationSpec = GhostMotion.MassSpring,
        label = "tilt"
    )

    this.graphicsLayer {
        if (enabled) {
            this.rotationX = rotationX
            cameraDistance = 12f * density
        }
    }
}
