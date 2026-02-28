package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.UserProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarView(
    nodes: Map<String, UserProfile>,
    meshHealth: Int,
    onNodeClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width.coerceAtMost(size.height) / 2 * 0.8f

            drawCircle(color = outlineColor, radius = maxRadius * 0.25f, center = center, style = Stroke(1f))
            drawCircle(color = outlineColor, radius = maxRadius * 0.5f, center = center, style = Stroke(1f))
            drawCircle(color = outlineColor, radius = maxRadius * 0.75f, center = center, style = Stroke(1f))
            drawCircle(color = outlineColor, radius = maxRadius, center = center, style = Stroke(2f))

            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha * 0.3f),
                radius = maxRadius * pulseRadius,
                center = center,
                style = Stroke(2f)
            )

            val angleRad = Math.toRadians(scanAngle.toDouble())
            val lineEnd = Offset(
                center.x + (maxRadius * cos(angleRad)).toFloat(),
                center.y + (maxRadius * sin(angleRad)).toFloat()
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, Color.Transparent),
                    start = lineEnd,
                    end = center
                ),
                start = center,
                end = lineEnd,
                strokeWidth = 4f
            )
        }

        nodes.values.forEachIndexed { index, node ->
            val angle = (index * 360f / nodes.size.coerceAtLeast(1)) + 45f
            val distance = 0.4f + (0.4f * (index % 3) / 3f)

            val angleRad = Math.toRadians(angle.toDouble())
            val infiniteTransitionNode = rememberInfiniteTransition(label = "nodeGlow")
            val nodeAlpha by infiniteTransitionNode.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nodeAlpha"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = (cos(angleRad) * 150 * distance).dp,
                        y = (sin(angleRad) * 150 * distance).dp
                    )
                    .size(64.dp)
                    .clickable { onNodeClick(node.id, node.name) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(node.color).copy(alpha = nodeAlpha), Color.Transparent)
                                )
                            )
                            .border(1.dp, Color(node.color).copy(alpha = nodeAlpha), CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        node.name.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = nodeAlpha),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .spectralGlow(primaryColor, radius = 12.dp, shape = CircleShape)
                .background(Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }
    }
}
