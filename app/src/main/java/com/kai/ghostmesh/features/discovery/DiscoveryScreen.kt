@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.kai.ghostmesh.features.discovery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.core.ui.theme.GhostMotion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var selectedTransports by remember { mutableStateOf(setOf("LAN", "Bluetooth")) }
    var telemetryNode by remember { mutableStateOf<UserProfile?>(null) }
    var interactingIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()

    val filteredNodes = remember(connectedNodes, selectedTransports) {
        connectedNodes.values.filter { it.transportType in selectedTransports }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {


        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    MediumTopAppBar(
                        title = { Text("NETWORK NODES", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge, letterSpacing = 1.sp) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    TransportFilterChips(selectedTransports, cornerRadius) { selectedTransports = it }
                }
            },
            floatingActionButton = {
                MorphingDiscoveryButton(onClick = { onShout("PING") })
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (filteredNodes.isEmpty()) {
                    EmptyDiscoveryState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(filteredNodes, key = { _, node -> node.id }) { index, node ->
                            val isNeighborInteracting = interactingIndex != -1 && interactingIndex != index

                            // Adaptive radius calculation
                            var previousOffset by remember { mutableIntStateOf(0) }
                            val currentOffset = listState.firstVisibleItemScrollOffset
                            val velocity = kotlin.math.abs(currentOffset - previousOffset)
                            LaunchedEffect(currentOffset) { previousOffset = currentOffset }

                            val absVelocity = velocity.coerceAtMost(100)
                            val dynamicRadius = (cornerRadius - (absVelocity * 0.15f)).coerceAtLeast(12f)

                            DiscoveryRow(
                                node = node,
                                isInteracting = interactingIndex == index,
                                modifier = Modifier
                                    .proximityDisplacement(isNeighborInteracting)
                                    .clip(RoundedCornerShape(dynamicRadius.dp)),
                                onInteracting = { interactingIndex = if (it) index else -1 },
                                onClick = { onNodeClick(node.id, node.name) },
                                onLongClick = { telemetryNode = node },
                                userRadius = dynamicRadius.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    telemetryNode?.let { node ->
        SurgicalOverlay(node, cornerRadius) { telemetryNode = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportFilterChips(selected: Set<String>, cornerRadius: Int, onUpdate: (Set<String>) -> Unit) {
    val transports = listOf("Nearby", "WiFiDirect", "LAN", "Bluetooth", "Cloud")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(transports) { t ->
            val isSelected = t in selected
            val actualRadius = cornerRadius.coerceAtMost(32)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val next = if (isSelected) {
                        if (selected.size > 1) selected - t else selected
                    } else {
                        selected + t
                    }
                    onUpdate(next)
                },
                label = { Text(t, fontWeight = FontWeight.Bold) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                shape = RoundedCornerShape(actualRadius.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun DiscoveryRow(
    node: UserProfile,
    isInteracting: Boolean,
    modifier: Modifier = Modifier,
    onInteracting: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    userRadius: Int
) {
    val transportColor = when(node.transportType) {
        "LAN" -> Color(0xFF00E676)
        "WiFiDirect" -> Color(0xFF2979FF)
        "Nearby" -> Color(0xFFFFEA00)
        "Bluetooth" -> Color(0xFFE91E63)
        "Cloud" -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primary
    }

    val dynamicRadius by animateDpAsState(
        targetValue = if (isInteracting) (userRadius / 2).dp else userRadius.dp,
        animationSpec = GhostMotion.MassSpringDp,
        label = "radius"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(dynamicRadius),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        modifier = modifier
            .fillMaxWidth()
            .jellyClickable(onClick = onClick, onLongClick = { onInteracting(true) })
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(dynamicRadius))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = transportColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp).border(1.dp, transportColor.copy(alpha = 0.4f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(node.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = transportColor, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(node.id.take(16).uppercase() + "...", style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.alpha(0.6f))
                Text(node.transportType ?: "P2P", style = MaterialTheme.typography.labelSmall, color = transportColor, fontWeight = FontWeight.Black)
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = if (node.batteryLevel > 20) transportColor else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text("${node.batteryLevel}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(0.7f))
            }
        }
    }
}

@Composable
fun EmptyDiscoveryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MD3ELoadingIndicator()
            Spacer(Modifier.height(24.dp))
            Text(
                "AWAITING PEERS...",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SurgicalOverlay(node: UserProfile, userRadius: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PEER METRICS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                TelemetryItem("NODE_ID", node.id)
                TelemetryItem("ENDPOINT", node.bestEndpoint ?: "N/A")
                TelemetryItem("TRANSPORT", node.transportType ?: "MESH")
                TelemetryItem("VITAL_SIGN", "${node.batteryLevel}% POWER")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("CLOSE")
            }
        },
        shape = RoundedCornerShape(userRadius.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )
}

@Composable
fun TelemetryItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
