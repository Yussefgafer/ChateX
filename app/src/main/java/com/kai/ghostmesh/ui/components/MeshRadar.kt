package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun MeshRadarBackground(
    modifier: Modifier = Modifier,
    pulseColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
    ) {
        val center = size.center
        val maxRadius = size.minDimension / 1.5f

        val gridColor = pulseColor.copy(alpha = 0.05f)
        for (i in 1..4) {
            drawCircle(
                color = gridColor,
                radius = maxRadius * (i / 4f),
                center = center,
                style = Stroke(width = 1f)
            )
        }

        val pulses = listOf(pulseProgress, (pulseProgress + 0.5f) % 1f)
        pulses.forEach { p ->
            drawCircle(
                color = pulseColor.copy(alpha = (1f - p) * 0.2f),
                radius = maxRadius * p,
                center = center,
                style = Stroke(width = 4f)
            )
        }

        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                0.5f to pulseColor.copy(alpha = 0.1f),
                1f to pulseColor.copy(alpha = 0.4f),
                center = center
            ),
            startAngle = sweepAngle - 40f,
            sweepAngle = 40f,
            useCenter = true,
            size = size * 1.5f,
            topLeft = center - size.center * 1.5f
        )
    }
}

// 🚀 Removed shadowed 'minus' and 'times' extensions as they are already provided by Compose UI Geometry
