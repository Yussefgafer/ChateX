package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.*

@Composable
fun MorphingIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 48.dp,
    duration: Int = 2000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "morph")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val startShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 5,
            innerRadius = 0.5f,
            rounding = CornerRounding(0.4f)
        )
    }
    val endShape = remember {
        RoundedPolygon.circle(numVertices = 10)
    }
    val morph = remember { Morph(startShape, endShape) }
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                renderEffect = null // Avoid unnecessary effects on weak GPUs
                alpha = 0.99f
            }
    ) {
        path.reset()
        morph.toPath(progress, path.asAndroidPath())
        
        val scaleFactor = size.toPx() / 2.5f
        
        // Correcting the center coordinates for Canvas scope
        translate(left = center.x, top = center.y) {
            scale(scale = scaleFactor) {
                drawPath(path, color)
            }
        }
    }
}
