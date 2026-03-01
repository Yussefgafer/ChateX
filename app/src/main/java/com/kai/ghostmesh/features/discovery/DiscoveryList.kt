package com.kai.ghostmesh.features.discovery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import java.io.File

@Composable
fun DiscoveryList(
    nodes: Map<String, UserProfile>,
    routingTable: Map<String, com.kai.ghostmesh.core.mesh.Route>,
    onNodeClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    isHeavyLoad: Boolean = false
) {
    val onlineNodes = nodes.values.filter { it.isOnline }.sortedByDescending { it.reputation }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(onlineNodes, key = { it.id }) { node ->
            val route = routingTable[node.id]
            GhostRow(
                node = node,
                pathCost = route?.cost ?: 100f,
                onClick = { onNodeClick(node.id, node.name) },
                isHeavyLoad = isHeavyLoad
            )
        }
    }
}

@Composable
fun GhostRow(
    node: UserProfile,
    pathCost: Float,
    onClick: () -> Unit,
    isHeavyLoad: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val warningAlpha by if (!isHeavyLoad && node.secondaryRouteAvailable) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(node.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val imagePath = node.profileImageLocalPath
                if (imagePath != null && File(imagePath).exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        node.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(node.color),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        node.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (node.isMaster) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    node.id.take(8) + " | " + node.reputationLevel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                if (node.secondaryRouteAvailable) {
                    Text(
                        "BACKUP ROUTE READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800).copy(alpha = warningAlpha),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SignalIndicator(pathCost = pathCost)
        }
    }
}

@Composable
fun SignalIndicator(pathCost: Float) {
    val (icon, color) = when {
        pathCost < 10f -> Icons.Default.SignalCellularAlt to Color(0xFF00E676)
        pathCost < 30f -> Icons.Default.SignalCellularAlt2Bar to Color(0xFF2979FF)
        pathCost < 60f -> Icons.Default.SignalCellularAlt1Bar to Color(0xFFFFEA00)
        else -> Icons.Default.SignalCellularConnectedNoInternet0Bar to Color(0xFFFF1744)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            pathCost.toInt().toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 8.sp
        )
    }
}
