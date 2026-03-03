package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Jelly Clickable: Non-uniform scaling for a physical "squishy" effect.
 */
fun Modifier.jellyClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val springSpec = spring<Float>(
        stiffness = 200f,
        dampingRatio = 0.4f
    )

    val widthScale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = springSpec,
        label = "jelly_w"
    )
    val heightScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = springSpec,
        label = "jelly_h"
    )

    this
        .graphicsLayer {
            scaleX = widthScale
            scaleY = heightScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

fun Modifier.magneticEffect(
    interactionSource: MutableInteractionSource
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "magnetic_squish"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.physicalTilt(
    enabled: Boolean = true
) = this // Simplified for Discovery Hub list performance

const val StiffnessLow = 120f
const val StiffnessMediumLow = 400f
