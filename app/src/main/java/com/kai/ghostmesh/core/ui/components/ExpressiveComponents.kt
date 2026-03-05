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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.*
import kotlin.math.min
import com.kai.ghostmesh.core.ui.theme.GhostMotion

private const val DURATION_PER_SHAPE_MS = 650

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

@Composable
fun CoercedExpressiveCard(
    userRadius: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var heightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val actualRadius = remember(userRadius, heightPx) {
        val heightDp = with(density) { heightPx.toDp() }.value
        if (heightDp > 0) userRadius.coerceAtMost(heightDp / 2f) else userRadius
    }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(actualRadius.dp),
        color = containerColor,
        modifier = modifier
            .onGloballyPositioned { heightPx = it.size.height }
            .then(if (onClick != null) Modifier.jellyClickable(onClick = onClick) else Modifier)
            .clip(RoundedCornerShape(actualRadius.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(actualRadius.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
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
        animationSpec = GhostMotion.TactileSpring,
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
                        color = Color.White.copy(alpha = 0.2f),
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
        animationSpec = GhostMotion.TactileSpring,
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
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
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

    val shapes = remember {
        listOf(
            RoundedPolygon.circle(numVertices = 8),
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.7f),
            RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.circle(numVertices = 5),
            RoundedPolygon.pill(),
            RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.8f),
            RoundedPolygon.circle(numVertices = 12),
            RoundedPolygon.rectangle(width = 2f, height = 1.5f, rounding = CornerRounding(0.5f)),
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.6f),
            RoundedPolygon.circle(numVertices = 4)
        )
    }

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = (shapes.size - 1) * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morph_factor"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localProgress = morphFactor - index.toFloat()

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }

    Box(
        modifier = modifier
            .size(72.dp)
            .jellyClickable(onClick = onClick)
            .drawWithCache {
                val path = morph.toPath(localProgress)
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
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

    val shapes = remember {
        listOf(
            RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.9f, rounding = CornerRounding(0.4f)),
            RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(0.2f)),
            RoundedPolygon.circle(numVertices = 5),
            RoundedPolygon.pill(),
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.7f, rounding = CornerRounding(0.5f)),
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.8f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.rectangle(width = 2f, height = 1.4f, rounding = CornerRounding(0.7f))
        )
    }

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = (shapes.size - 1) * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_morph"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localProgress = morphFactor - index.toFloat()

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }

    Box(
        modifier = modifier
            .size(48.dp)
            .drawWithCache {
                val path = morph.toPath(localProgress)
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
