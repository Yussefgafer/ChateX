package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
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
    val isPowerSaveMode = meshHealth > 90 || nodes.size > 20

    val pulseAlpha by if (isPowerSaveMode) remember { mutableStateOf(0.1f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseAlpha"
        )
    }

    val pulseRadius by if (isPowerSaveMode) remember { mutableStateOf(0.5f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseRadius"
        )
    }

    val linkPulse by if (isPowerSaveMode) remember { mutableStateOf(1.0f) } else {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "linkPulse"
        )
    }

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

            // Draw Links
            val nodeEntries = nodes.values.toList()
            nodeEntries.forEachIndexed { index, node ->
                val baseAngle = (index * 360f / nodeEntries.size.coerceAtLeast(1)) + 45f
                val distanceRatio = 0.4f + (0.4f * (index % 3) / 3f)
                val angleRad = Math.toRadians(baseAngle.toDouble())

                val nodePos = Offset(
                    center.x + (cos(angleRad) * 150 * distanceRatio).dp.toPx(),
                    center.y + (sin(angleRad) * 150 * distanceRatio).dp.toPx()
                )

                // Link to Center (Me)
                val transport = node.bestEndpoint?.split(":")?.firstOrNull() ?: "Unknown"
                val linkColor = when(transport) {
                    "LAN" -> Color(0xFF00E676)
                    "WiFiDirect" -> Color(0xFF2979FF)
                    "Nearby" -> Color(0xFFFFEA00)
                    "Bluetooth" -> Color(0xFFFF1744)
                    else -> primaryColor.copy(alpha = 0.5f)
                }

                val linkWeight = (node.batteryLevel / 100f).coerceIn(0.2f, 1f)

                drawLine(
                    color = linkColor,
                    start = center,
                    end = nodePos,
                    strokeWidth = (2.dp.toPx() * linkWeight * linkPulse),
                    alpha = (0.2f + (0.5f * linkWeight)) * linkPulse
                )
            }
        }

        nodes.values.forEachIndexed { index, node ->
            val baseAngle = (index * 360f / nodes.size.coerceAtLeast(1)) + 45f
            val distance = 0.4f + (0.4f * (index % 3) / 3f)

            // Magnetic "Float"
            val floatX by if (isPowerSaveMode) remember { mutableStateOf(0f) } else {
                infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000 + index * 100, easing = SineToSineEasing()),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "nodeFloatX"
                )
            }
            val floatY by if (isPowerSaveMode) remember { mutableStateOf(0f) } else {
                infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3500 + index * 100, easing = SineToSineEasing()),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "nodeFloatY"
                )
            }

            val angleRad = Math.toRadians(baseAngle.toDouble())

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            ((cos(angleRad) * 150 * distance).dp.roundToPx() + floatX.dp.roundToPx()),
                            ((sin(angleRad) * 150 * distance).dp.roundToPx() + floatY.dp.roundToPx())
                        )
                    }
                    .size(64.dp)
                    .semantics { contentDescription = "Node: ${node.name}, Battery: ${node.batteryLevel}%" }
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
                    // Battery indicator
                    Text(
                        "${node.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if(node.batteryLevel < 15) Color.Red else MaterialTheme.colorScheme.outline,
                        fontSize = 8.sp
                    )
                }
            }
        }

        // Center Node (Me)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.1f))
                .semantics { contentDescription = "My Spectral Node" },
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
