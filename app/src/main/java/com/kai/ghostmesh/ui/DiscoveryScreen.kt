package com.kai.ghostmesh.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.model.UserProfile
import com.kai.ghostmesh.ui.components.RadarView
import com.kai.ghostmesh.ui.components.physicalTilt
import com.kai.ghostmesh.ui.components.magneticClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int = 16,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var isRadarMode by remember { mutableStateOf(true) }
    var showShoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Discovery Hub")
                        Text(
                            "MESH ACTIVE • ${connectedNodes.size} NODES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isRadarMode = !isRadarMode }) {
                        Icon(if (isRadarMode) Icons.Default.TableRows else Icons.Default.CellTower, null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showShoutDialog = true },
                icon = { Icon(Icons.Default.Podcasts, null) },
                text = { Text("GLOBAL SHOUT") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.physicalTilt()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            HealthBanner(meshHealth)

            if (isRadarMode) {
                RadarView(nodes = connectedNodes, meshHealth = meshHealth, onNodeClick = onNodeClick)
            } else {
                if (connectedNodes.isEmpty()) {
                    EmptyDiscoveryView()
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val nodesList = connectedNodes.values.toList()
                        items(nodesList.size, key = { nodesList[it].id }) { index ->
                            val node = nodesList[index]
                            NodeCard(node = node, cornerRadius = cornerRadius, onClick = { onNodeClick(node.id, node.name) })
                        }
                    }
                }
            }
        }
    }

    if (showShoutDialog) {
        var shoutText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showShoutDialog = false },
            title = {
                Text(
                    "GLOBAL TRANSMISSION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = shoutText,
                    onValueChange = { shoutText = it },
                    placeholder = { Text("Transmit message...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { onShout(shoutText); showShoutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("TRANSMIT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShoutDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun HealthBanner(health: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SignalCellularAlt, 
                null, 
                tint = if (health > 50) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Mesh Integrity: $health%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NodeCard(node: UserProfile, cornerRadius: Int = 24, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .physicalTilt()
            .magneticClickable(onClick),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(node.color).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        node.name.take(1).uppercase(),
                        color = Color(node.color),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
            
            Column {
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    node.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyDiscoveryView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CellTower, 
                null, 
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Searching for peers...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
