package com.kai.ghostmesh.features.discovery

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.compose.animation.core.*
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.LazyRow
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var selectedTransport by remember { mutableStateOf("ALL") }
    var telemetryNode by remember { mutableStateOf<UserProfile?>(null) }
    val listState = rememberLazyListState()

    val time = rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing))
    )

    val filteredNodes = remember(connectedNodes, selectedTransport) {
        if (selectedTransport == "ALL") connectedNodes.values.toList()
        else connectedNodes.values.filter { it.transportType == selectedTransport }
    }

    val velocity = remember { derivedStateOf { abs(listState.firstVisibleItemScrollOffset.toFloat() % 100) } }
    val dynamicRadius by animateDpAsState(
        targetValue = (24 + (velocity.value / 15)).dp.coerceAtMost(36.dp),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f),
        label = "discovery_radius"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = { Text("Discovery Hub", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    TransportFilterChips(selectedTransport) { selectedTransport = it }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (filteredNodes.isEmpty()) {
                    EmptyDiscoveryState(time.value)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredNodes, key = { it.id }) { node ->
                            DiscoveryRow(
                                node = node,
                                cornerRadius = dynamicRadius,
                                onClick = { onNodeClick(node.id, node.name) },
                                onLongClick = { telemetryNode = node }
                            )
                        }
                    }
                }
            }
        }
    }

    telemetryNode?.let { node ->
        SurgicalOverlay(node) { telemetryNode = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportFilterChips(selected: String, onSelect: (String) -> Unit) {
    val transports = listOf("ALL", "Nearby", "WiFiDirect", "LAN", "Bluetooth", "Cloud")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(transports) { t ->
            FilterChip(
                selected = selected == t,
                onClick = { onSelect(t) },
                label = { Text(t) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.physicalTilt(),
                leadingIcon = if (selected == t) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
        }
    }
}

@Composable
fun DiscoveryRow(
    node: UserProfile,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val transportColor = when(node.transportType) {
        "LAN" -> Color(0xFF00E676)
        "WiFiDirect" -> Color(0xFF2979FF)
        "Nearby" -> Color(0xFFFFEA00)
        "Bluetooth" -> Color(0xFFFF1744)
        "Cloud" -> Color(0xFFBB86FC)
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(transportColor.copy(alpha = 0.15f))
                )
                Text(node.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = transportColor, style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(node.id.take(16), style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.alpha(0.6f))
                Text(node.transportType ?: "P2P", style = MaterialTheme.typography.labelSmall, color = transportColor)
            }

            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = if (node.batteryLevel > 20) transportColor else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EmptyDiscoveryState(time: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Scanning for network nodes...",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.width(120.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SurgicalOverlay(node: UserProfile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Peer Telemetry", fontWeight = FontWeight.Black) },
        text = {
            Column {
                TelemetryItem("Endpoint", node.bestEndpoint ?: "Unknown")
                TelemetryItem("Transport", node.transportType ?: "P2P")
                TelemetryItem("Battery", "${node.batteryLevel}%")
                TelemetryItem("NodeID", node.id)
            }
        },
        confirmButton = {
            ExpressiveButton(onClick = onDismiss) { Text("CLOSE") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun TelemetryItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.labelMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}
