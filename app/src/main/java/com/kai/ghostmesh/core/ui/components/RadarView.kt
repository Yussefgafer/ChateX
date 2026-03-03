package com.kai.ghostmesh.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.R
import com.kai.ghostmesh.core.model.UserProfile
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
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

    val pulseAlpha = if (isPowerSaveMode) remember { mutableStateOf(0.1f) } else {
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

    val pulseRadius = if (isPowerSaveMode) remember { mutableStateOf(0.5f) } else {
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

    val radarDesc = stringResource(R.string.radar_view_description, nodes.size)
    val myNodeDesc = stringResource(R.string.my_node_description)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { contentDescription = radarDesc },
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width.coerceAtMost(size.height) / 2 * 0.8f

            drawCircle(color = outlineColor, radius = maxRadius * 0.25f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius * 0.5f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius * 0.75f, center = center, style = Stroke(0.5.dp.toPx()))
            drawCircle(color = outlineColor, radius = maxRadius, center = center, style = Stroke(1.dp.toPx()))

            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha.value),
                radius = maxRadius * pulseRadius.value,
                center = center,
                style = Stroke(1.5.dp.toPx())
            )
        }

        nodes.values.forEachIndexed { index, node ->
            key(node.id) {
                val interactionSource = remember { MutableInteractionSource() }
                val baseAngle = (index * 360f / nodes.size.coerceAtLeast(1)) + 45f
                val distance = 0.4f + (0.4f * (index % 3) / 3f)

                val angleRad = Math.toRadians(baseAngle.toDouble())
                val peerNodeDesc = stringResource(R.string.peer_node_description, node.name, node.batteryLevel)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cos(angleRad) * 150 * distance).dp.roundToPx(),
                                (sin(angleRad) * 150 * distance).dp.roundToPx()
                            )
                        }
                    .size(64.dp)
                    .semantics { contentDescription = peerNodeDesc }
                    .physicalTilt()
                    .magneticEffect(interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onNodeClick(node.id, node.name) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MorphingNode(
                        color = Color(node.color),
                        modifier = Modifier.size(16.dp),
                        energy = node.batteryLevel / 100f
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
    }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.1f))
                .semantics { contentDescription = myNodeDesc },
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
