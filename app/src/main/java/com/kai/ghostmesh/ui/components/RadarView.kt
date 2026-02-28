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
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
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

            // MD3 concentric circles
            drawCircle(color = outlineColor, radius = maxRadius * 0.25f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius * 0.5f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius * 0.75f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius, center = center, style = Stroke(1.dp.toPx()))

            // Subtle pulse
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = maxRadius * pulseRadius,
                center = center,
                style = Stroke(1.5.dp.toPx())
            )
        }

        nodes.values.forEachIndexed { index, node ->
            val baseAngle = (index * 360f / nodes.size.coerceAtLeast(1)) + 45f
            val distance = 0.4f + (0.4f * (index % 3) / 3f)

            // Magnetic "Float"
            val floatX by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000 + index * 100, easing = SineToSineEasing()),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nodeFloatX"
            )
            val floatY by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3500 + index * 100, easing = SineToSineEasing()),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nodeFloatY"
            )

            val angleRad = Math.toRadians(baseAngle.toDouble())

            Box(
                modifier = Modifier
                    .offset(
                        x = (cos(angleRad) * 150 * distance).dp + floatX.dp,
                        y = (sin(angleRad) * 150 * distance).dp + floatY.dp
                    )
                    .size(64.dp)
                    .magneticClickable({ onNodeClick(node.id, node.name) }),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(node.color))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        node.name.take(8),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Center Node (Me)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }
    }
}

fun SineToSineEasing() = Easing { fraction ->
    ((Math.sin(fraction * Math.PI - Math.PI / 2) + 1) / 2).toFloat()
}
