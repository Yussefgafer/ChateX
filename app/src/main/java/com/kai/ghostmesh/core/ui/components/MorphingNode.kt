package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun MorphingNode(
    color: Color,
    modifier: Modifier = Modifier,
    isInteracted: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "morph")
    val progress by if (isInteracted) {
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(stiffness = StiffnessMediumLow),
            label = "interactedMorph"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "idleMorph"
        )
    }

    val shapeA = remember {
        RoundedPolygon(numVertices = 6)
    }
    val shapeB = remember {
        RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.7f, rounding = androidx.graphics.shapes.CornerRounding(0.2f))
    }
    val morph = remember(shapeA, shapeB) {
        Morph(shapeA, shapeB)
    }

    val matrix = remember { android.graphics.Matrix() }

    Canvas(modifier = modifier.size(16.dp)) {
        val path = morph.toPath(progress).asComposePath()
        val scale = size.minDimension / 2f
        matrix.reset()
        matrix.setScale(scale, scale)
        path.asAndroidPath().transform(matrix)

        translate(size.width / 2f, size.height / 2f) {
            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }
    }
}
