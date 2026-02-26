package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
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

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Hint to the system to use GPU layers
                alpha = 0.99f 
            }
    ) {
        val center = size.center
        val maxRadius = size.minDimension / 1.5f

        // 1. Static Grid (Drawn once per cache invalidate)
        val gridColor = pulseColor.copy(alpha = 0.05f)
        for (i in 1..4) {
            drawCircle(
                color = gridColor,
                radius = maxRadius * (i / 4f),
                center = center,
                style = Stroke(width = 1f)
            )
        }

        // 2. Optimized Dynamic Pulses
        // Instead of 2 separate animators, we offset the same progress
        val pulses = listOf(pulseProgress, (pulseProgress + 0.5f) % 1f)
        
        pulses.forEach { p ->
            drawCircle(
                color = pulseColor.copy(alpha = (1f - p) * 0.3f),
                radius = maxRadius * p,
                center = center,
                style = Stroke(width = 4f)
            )
        }
    }
}
