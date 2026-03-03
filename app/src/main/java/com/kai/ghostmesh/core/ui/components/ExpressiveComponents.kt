package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (isPressed) MaterialTheme.motionScheme.fastSpatialSpec() else MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "button_morph"
    )

    val shapeA = remember { RoundedPolygon(numVertices = 8, rounding = androidx.graphics.shapes.CornerRounding(0.4f)) }
    val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.92f, rounding = androidx.graphics.shapes.CornerRounding(0.1f)) }
    val morph = remember(shapeA, shapeB) { Morph(shapeA, shapeB) }

    val matrix = remember { android.graphics.Matrix() }

    Button(
        onClick = onClick,
        modifier = modifier
            .magneticClickable(onClick, enabled)
            .graphicsLayer {
                val scale = if (isPressed) 0.96f else 1f
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = morph.toPath(morphProgress).asComposePath()
                matrix.reset()
                val scale = size.minDimension / 2f
                matrix.setScale(scale, scale)
                matrix.postTranslate(size.width / 2f, size.height / 2f)
                path.asAndroidPath().transform(matrix)
                return Outline.Generic(path)
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        thumb = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val morphProgress by animateFloatAsState(
                targetValue = if (isPressed) 1f else 0f,
                animationSpec = spring(stiffness = StiffnessMediumLow),
                label = "thumb_morph"
            )

            val shapeA = remember { RoundedPolygon.circle(numVertices = 6) }
            val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.6f) }
            val morph = remember(shapeA, shapeB) { Morph(shapeA, shapeB) }
            val matrix = remember { android.graphics.Matrix() }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        val scale = if (isPressed) 1.4f else 1f
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                val thumbColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = morph.toPath(morphProgress).asComposePath()
                    matrix.reset()
                    val scale = size.minDimension / 2f
                    matrix.setScale(scale, scale)
                    matrix.postTranslate(size.width / 2f, size.height / 2f)
                    path.asAndroidPath().transform(matrix)
                    drawPath(path, color = thumbColor, style = Fill)
                }
            }
        }
    )
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = StiffnessLow),
        label = "icon_morph"
    )

    val shapeA = remember { RoundedPolygon.circle(numVertices = 4) }
    val shapeB = remember { RoundedPolygon(numVertices = 4, rounding = androidx.graphics.shapes.CornerRounding(0.1f)) }
    val morph = remember(shapeA, shapeB) { Morph(shapeA, shapeB) }
    val matrix = remember { android.graphics.Matrix() }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .magneticClickable(onClick, enabled)
            .graphicsLayer {
                val scale = if (isPressed) 0.9f else 1f
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (containerColor != Color.Transparent) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = morph.toPath(morphProgress).asComposePath()
                    matrix.reset()
                    val scale = size.minDimension / 2f
                    matrix.setScale(scale, scale)
                    matrix.postTranslate(size.width / 2f, size.height / 2f)
                    path.asAndroidPath().transform(matrix)
                    drawPath(path, color = containerColor, style = Fill)
                }
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
    }
}
