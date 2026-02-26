package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun MeshNode(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nodeFloat")
    
    // Smooth 90Hz floating animation
    val floatX by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = SineToSineEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = SineToSineEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = floatX.dp.toPx()
                translationY = floatY.dp.toPx()
                // Use shadows sparingly or use a cheaper alternative
            }
            .size(100.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    0.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    1.0f to Color.Transparent
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(size = 40.dp, color = MaterialTheme.colorScheme.primary)
            Text(
                text = name,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// Custom easing for smooth floating
fun SineToSineEasing() = Easing { fraction ->
    ((Math.sin(fraction * Math.PI - Math.PI / 2) + 1) / 2).toFloat()
}
