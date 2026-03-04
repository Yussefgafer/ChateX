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

/**
 * Jelly Clickable: Hardened "Magnetic Snapping" physics for professional response.
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
        stiffness = Spring.StiffnessMedium,
        dampingRatio = 0.85f
    )

    val widthScale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1f,
        animationSpec = springSpec,
        label = "jelly_w"
    )
    val heightScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = springSpec,
        label = "jelly_h"
    )

    this
        .graphicsLayer {
            scaleX = widthScale
            scaleY = heightScale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            onLongClick = onLongClick
        )
}

/**
 * Magnetic Effect: High-damping "Snapping" for expressive interaction.
 */
fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val springSpec = spring<Float>(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = 0.85f
    )

    val widthScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = springSpec,
        label = "mag_w"
    )
    val heightScale by animateFloatAsState(
        targetValue = if (isPressed) 1.02f else 1f,
        animationSpec = springSpec,
        label = "mag_h"
    )

    this.graphicsLayer {
        scaleX = widthScale
        scaleY = heightScale
    }
}

fun Modifier.physicalTilt(
    enabled: Boolean = true
) = this // Simplified for performance on low-RAM devices

const val StiffnessLow = 120f
const val StiffnessMediumLow = 400f
