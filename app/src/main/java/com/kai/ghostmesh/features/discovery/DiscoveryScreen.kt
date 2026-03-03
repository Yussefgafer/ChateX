package com.kai.ghostmesh.features.discovery

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.*
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var shoutText by remember { mutableStateOf("") }
    var selectedTransport by remember { mutableStateOf("ALL") }
    var telemetryNode by remember { mutableStateOf<UserProfile?>(null) }

    val time = rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing))
    )

    val filteredNodes = remember(connectedNodes, selectedTransport) {
        if (selectedTransport == "ALL") connectedNodes.values.toList()
        else connectedNodes.values.filter { it.transportType == selectedTransport }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Discovery Hub", fontWeight = FontWeight.Bold) }
                )
                TransportFilterRow(selectedTransport) { selectedTransport = it }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filteredNodes.isEmpty()) {
                EmptyDiscoveryState(time.value)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNodes, key = { it.id }) { node ->
                        DiscoveryRow(
                            node = node,
                            onClick = { onNodeClick(node.id, node.name) },
                            onLongClick = { telemetryNode = node }
                        )
                    }
                }
            }
        }
    }

    telemetryNode?.let { node ->
        SurgicalOverlay(node) { telemetryNode = null }
    }
}

@Composable
fun TransportFilterRow(selected: String, onSelect: (String) -> Unit) {
    val transports = listOf("ALL", "Nearby", "WiFiDirect", "LAN", "Bluetooth")
    ScrollableTabRow(
        selectedTabIndex = transports.indexOf(selected).coerceAtLeast(0),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {}
    ) {
        transports.forEach { t ->
            Tab(
                selected = selected == t,
                onClick = { onSelect(t) },
                text = { Text(t, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
fun DiscoveryRow(
    node: UserProfile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val transportColor = when(node.transportType) {
        "LAN" -> Color(0xFF00E676)
        "WiFiDirect" -> Color(0xFF2979FF)
        "Nearby" -> Color(0xFFFFEA00)
        "Bluetooth" -> Color(0xFFFF1744)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .jellyClickable(onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(transportColor.copy(alpha = 0.15f))
                )
                Text(node.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = transportColor)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(node.id.take(16), style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.alpha(0.6f))
            }

            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = if (node.batteryLevel > 20) transportColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyDiscoveryState(time: Float) {
    val brush = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GhostShaders.createNoiseBrush(time, 1000f, 1000f) ?: SolidColor(MaterialTheme.colorScheme.surface)
    } else {
        SolidColor(MaterialTheme.colorScheme.surface)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Searching the network...",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun SurgicalOverlay(node: UserProfile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Node Telemetry") },
        text = {
            Column {
                TelemetryItem("Endpoint", node.bestEndpoint ?: "Unknown")
                TelemetryItem("Transport", node.transportType ?: "P2P")
                TelemetryItem("Battery", "${node.batteryLevel}%")
                TelemetryItem("NodeID", node.id)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
fun TelemetryItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.labelMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}
