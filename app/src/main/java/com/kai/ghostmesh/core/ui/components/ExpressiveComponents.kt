package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.*
import kotlin.math.min

/**
 * MD3E Primitive: Morphing Shape Background
 */
@Composable
fun MorphingShapeBackground(
    progress: Float,
    shape1: RoundedPolygon,
    shape2: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier
) {
    val morph = remember(shape1, shape2) { Morph(shape1, shape2) }

    Box(
        modifier = modifier.drawWithCache {
            val path = morph.toPath(progress)
            val matrix = Matrix()
            val sizeMin = min(size.width, size.height)
            matrix.scale(sizeMin / 2f, sizeMin / 2f)
            matrix.translate(1f, 1f)
            path.transform(matrix)

            onDrawBehind {
                drawPath(path, color = color)
            }
        }
    )
}

/**
 * Expressive Card: A high-depth container with "Elastic" borders.
 */
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "card_morph"
    )

    val shapeStart = remember { RoundedPolygon.circle(numVertices = 4) }
    val shapeEnd = remember { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.1f)) }
    val morph = remember { Morph(shapeStart, shapeEnd) }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        interactionSource = interactionSource,
        color = Color.Transparent,
        modifier = modifier
            .physicalTilt()
            .drawWithCache {
                val path = morph.toPath(morphProgress)
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = containerColor)
                    drawPath(
                        path,
                        color = Color.White.copy(alpha = 0.15f),
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

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

    val shapeProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "btn_morph"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .jellyClickable(onClick = onClick, enabled = enabled)
            .drawWithCache {
                val s1 = RoundedPolygon.pill()
                val s2 = RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.2f))
                val morph = Morph(s1, s2)
                val path = morph.toPath(shapeProgress)
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = if (enabled) containerColor else containerColor.copy(alpha = 0.3f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

@Composable
fun MorphingDiscoveryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_morph")

    // 10 distinct shapes for the FAB
    val shapes = remember {
        listOf(
            RoundedPolygon.circle(numVertices = 8),
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.7f),
            RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.circle(numVertices = 5), // Pentagon-ish
            RoundedPolygon.pill(),
            RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.8f),
            RoundedPolygon.circle(numVertices = 12),
            RoundedPolygon.rectangle(width = 2f, height = 1.5f, rounding = CornerRounding(0.5f)),
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.6f),
            RoundedPolygon.circle(numVertices = 4) // Diamond/Square
        )
    }

    val emphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // MD3 Emphasized

    val index by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = shapes.size - 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5000 // Macro duration for full cycle
                for (i in shapes.indices) {
                    i at (i * 500) using emphasizedEasing
                }
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "shape_index"
    )

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = emphasizedEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    val currentShape = shapes[index]
    val nextShape = shapes[(index + 1) % shapes.size]
    val morph = remember(currentShape, nextShape) { Morph(currentShape, nextShape) }

    Box(
        modifier = modifier
            .size(72.dp)
            .jellyClickable(onClick = onClick)
            .drawWithCache {
                val path = morph.toPath(progress)
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = containerColor)
                    drawPath(path, color = contentColor.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Radar, "Discovery", tint = contentColor, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun MD3ELoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_morph")

    // Sequence: SOFT_BURST → COOKIE_9 → PENTAGON → PILL → SUNNY → COOKIE_4 → OVAL
    val shapes = remember {
        listOf(
            RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.9f, rounding = CornerRounding(0.4f)), // SOFT_BURST
            RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(0.2f)), // COOKIE_9
            RoundedPolygon.circle(numVertices = 5), // PENTAGON
            RoundedPolygon.pill(), // PILL
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.7f, rounding = CornerRounding(0.5f)), // SUNNY
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.8f, rounding = CornerRounding(0.3f)), // COOKIE_4
            RoundedPolygon.rectangle(width = 2f, height = 1.4f, rounding = CornerRounding(0.7f)) // OVAL
        )
    }

    val index by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = shapes.size - 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                for (i in shapes.indices) {
                    i at (i * 500)
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_index"
    )

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_progress"
    )

    val currentShape = shapes[index]
    val nextShape = shapes[(index + 1) % shapes.size]
    val morph = remember(currentShape, nextShape) { Morph(currentShape, nextShape) }

    Box(
        modifier = modifier
            .size(48.dp)
            .drawWithCache {
                val path = morph.toPath(progress)
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = color)
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
    Box(
        modifier = modifier
            .size(48.dp)
            .jellyClickable(onClick = onClick, enabled = enabled)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
    }
}

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
        modifier = modifier.physicalTilt(),
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

fun Morph.toPath(progress: Float): Path {
    val androidPath = android.graphics.Path()
    this.toPath(progress, androidPath)
    return androidPath.asComposePath()
}

fun RoundedPolygon.Companion.pill(): RoundedPolygon {
    return RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.5f))
}
