package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.min

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

    Button(
        onClick = onClick,
        modifier = modifier
            .physicalTilt(enabled)
            .magneticEffect(interactionSource),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        content = content
    )
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
        modifier = modifier,
        valueRange = valueRange,
        steps = steps
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

    IconButton(
        onClick = onClick,
        modifier = modifier
            .physicalTilt(enabled)
            .magneticEffect(interactionSource),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (containerColor != Color.Transparent) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = containerColor,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
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
    val infiniteTransition = rememberInfiniteTransition(label = "discovery_morph")

    // Mandate: Spring Physics (Damping 0.85) - for cyclic we use keyframes but can mimic springiness
    // Actually, for truly expressive infinite morphing, we can use a sequence of shapes

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Using specialized shapes for "Peer Search/Discovery"
    val shape1 = remember { RoundedPolygon.circle(numVertices = 4) } // Squircle-ish
    val shape2 = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            innerRadius = 0.8f,
            rounding = CornerRounding(radius = 0.4f, smoothing = 0.5f)
        )
    }

    val morph = remember { Morph(shape1, shape2) }

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .physicalTilt()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .drawWithCache {
                val path = morph.toPath(progress)
                val matrix = Matrix()
                val sizeMin = min(size.width, size.height)
                matrix.scale(sizeMin / 2f, sizeMin / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                onDrawBehind {
                    // Glassmorphism effect background (mandate: 0.5px border)
                    drawPath(path, color = containerColor)
                    drawPath(
                        path,
                        color = contentColor.copy(alpha = 0.2f),
                        style = Stroke(width = 0.5.dp.toPx())
                    )

                    withTransform({
                        rotate(rotation)
                    }) {
                        // Dynamic "Pulse" rings
                        drawCircle(
                            color = contentColor.copy(alpha = 0.1f * (1f - progress)),
                            radius = (sizeMin / 2f) * progress,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Radar,
            contentDescription = "Discovery",
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun MorphingSearchIcon(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transitionProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "morph_progress"
    )

    val rotation by animateFloatAsState(
        targetValue = if (active) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "rotation"
    )

    val searchShape = remember {
        RoundedPolygon.circle(numVertices = 40)
    }

    val closeShape = remember {
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.1f, rounding = CornerRounding(0.2f))
    }

    val morph = remember(searchShape, closeShape) {
        Morph(searchShape, closeShape)
    }

    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { rotationZ = rotation }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .drawWithCache {
                val path = morph.toPath(transitionProgress)
                val matrix = Matrix()
                val sizeMin = min(size.width, size.height)
                matrix.scale(sizeMin / 2.2f, sizeMin / 2.2f)
                matrix.translate(1.1f, 1.1f)
                path.transform(matrix)

                onDrawBehind {
                    drawPath(path, color = color, style = Stroke(width = 3.dp.toPx()))
                    if (transitionProgress < 0.5f) {
                        val alpha = (1f - transitionProgress * 2f).coerceIn(0f, 1f)
                        drawLine(
                            color = color.copy(alpha = alpha),
                            start = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f),
                            end = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.95f),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Content placeholder
    }
}

fun Morph.toPath(progress: Float): Path {
    val androidPath = android.graphics.Path()
    this.toPath(progress, androidPath)
    return androidPath.asComposePath()
}
