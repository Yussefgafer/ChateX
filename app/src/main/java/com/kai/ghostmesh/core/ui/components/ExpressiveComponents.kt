package com.kai.ghostmesh.core.ui.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import com.kai.ghostmesh.core.ui.theme.GhostMotion

const val DURATION_PER_SHAPE_MS = 650

object MaterialShapes {
    fun softBurst() = RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.7f, rounding = CornerRounding(0.3f))
    fun cookie9() = RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.85f, rounding = CornerRounding(0.15f))
    fun pentagon() = RoundedPolygon.circle(numVertices = 5)
    fun pill() = RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.5f))
    fun sunny() = RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.5f, rounding = CornerRounding(0.3f))
    fun cookie4() = RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.8f, rounding = CornerRounding(0.2f))
    fun oval() = RoundedPolygon.rectangle(width = 1.4f, height = 2f, rounding = CornerRounding(1f))
    fun diamond() = RoundedPolygon.circle(numVertices = 4)
    fun leaf() = RoundedPolygon.star(numVerticesPerRadius = 2, innerRadius = 0.4f, rounding = CornerRounding(0.8f))
    fun hexagon() = RoundedPolygon.circle(numVertices = 6)

    val IndeterminateSequence = listOf(
        softBurst(), cookie9(), pentagon(), pill(), sunny(), cookie4(), oval(), diamond(), leaf(), hexagon()
    )
    val LoadingSequence = listOf(
        softBurst(), cookie9(), pentagon(), pill(), sunny(), cookie4(), oval()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoercedExpressiveCard(
    userRadius: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = GhostMotion.TactileSpring,
        label = "card_morph"
    )

    val shapeStart = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(userRadius.coerceIn(0f, 100f) / 100f)) }
    val shapeEnd = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding((userRadius * 1.2f).coerceIn(0f, 100f) / 100f)) }
    val morph = remember(userRadius) { Morph(shapeStart, shapeEnd) }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        interactionSource = interactionSource,
        color = Color.Transparent,
        modifier = modifier
            .physicalTilt()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(16.dp)
                } else Modifier
            )
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    morph.toPath(morphProgress, path.asAndroidPath())

                    matrix.reset()
                    matrix.translate(1f, 1f)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    path.transform(matrix)

                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

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
                val matrix = Matrix()
                val s1 = MaterialShapes.pill()
                val s2 = RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.2f))
                val morph = Morph(s1, s2)

                onDrawBehind {
                    val path = Path()
                    morph.toPath(shapeProgress, path.asAndroidPath())

                    matrix.reset()
                    matrix.translate(1f, 1f)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    path.transform(matrix)

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingDiscoveryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_morph")
    val shapes = MaterialShapes.IndeterminateSequence

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = shapes.size * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
                shapes.size.toFloat() at (shapes.size * DURATION_PER_SHAPE_MS)
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morph_factor"
    )

    val linearFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(shapes.size * DURATION_PER_SHAPE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "linear_factor"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localEasedProgress = (morphFactor - index.toFloat()).coerceIn(0f, 1f)
    val localLinearProgress = (linearFactor - index.toFloat()).coerceIn(0f, 1f)

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }
    val animatedProgress by animateFloatAsState(targetValue = localEasedProgress, animationSpec = GhostMotion.MorphSpring)

    val rotation = (140f * index) + (50f * localLinearProgress) + (90f * localEasedProgress)

    Box(
        modifier = modifier
            .size(72.dp)
            .jellyClickable(onClick = onClick)
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    morph.toPath(animatedProgress, path.asAndroidPath())

                    matrix.reset()
                    matrix.translate(1f, 1f)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.rotateZ(rotation)
                    path.transform(matrix)

                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Radar, "Discovery", tint = contentColor, modifier = Modifier.size(32.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MD3ELoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_morph")
    val shapes = MaterialShapes.LoadingSequence

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = shapes.size * DURATION_PER_SHAPE_MS
                shapes.indices.forEach { i ->
                    i.toFloat() at (i * DURATION_PER_SHAPE_MS) using GhostMotion.EmphasizedEasing
                }
                shapes.size.toFloat() at (shapes.size * DURATION_PER_SHAPE_MS)
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_morph"
    )

    val linearFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (shapes.size).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(shapes.size * DURATION_PER_SHAPE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_linear"
    )

    val index = morphFactor.toInt().coerceIn(0, shapes.size - 1)
    val nextIndex = (index + 1) % shapes.size
    val localEasedProgress = (morphFactor - index.toFloat()).coerceIn(0f, 1f)
    val localLinearProgress = (linearFactor - index.toFloat()).coerceIn(0f, 1f)

    val currentShape = shapes[index]
    val nextShape = shapes[nextIndex]
    val morph = remember(index, nextIndex) { Morph(currentShape, nextShape) }
    val animatedProgress by animateFloatAsState(targetValue = localEasedProgress, animationSpec = GhostMotion.MorphSpring)

    val rotation = (140f * index) + (50f * localLinearProgress) + (90f * localEasedProgress)

    Box(
        modifier = modifier
            .size(48.dp)
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val path = Path()
                    morph.toPath(animatedProgress, path.asAndroidPath())

                    matrix.reset()
                    matrix.translate(1f, 1f)
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.rotateZ(rotation)
                    path.transform(matrix)

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

fun RoundedPolygon.Companion.pill(): RoundedPolygon {
    return RoundedPolygon.rectangle(width = 2f, height = 1f, rounding = CornerRounding(0.5f))
}
