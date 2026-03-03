package com.kai.ghostmesh.features.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var shoutText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radar, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Spectral Discovery", fontWeight = FontWeight.Black)
                    }
                },
                actions = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                        Text("${meshHealth}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ElectricBolt, null, modifier = Modifier.size(16.dp), tint = if (meshHealth > 50) Color.Green else Color.Yellow)
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = shoutText,
                        onValueChange = { shoutText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Shout into the void...") },
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.width(8.dp))
                    ExpressiveIconButton(onClick = { if(shoutText.isNotBlank()) { onShout(shoutText); shoutText = "" } }) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                RadarView(
                    nodes = connectedNodes,
                    meshHealth = meshHealth,
                    onNodeClick = onNodeClick
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item { Text("Spectral Nodes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                items(connectedNodes.values.toList()) { ghost ->
                    GhostNodeItem(ghost, onClick = { onNodeClick(ghost.id, ghost.name) })
                }
            }
        }
    }
}

@Composable
fun GhostNodeItem(profile: UserProfile, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(profile.name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(profile.status) },
        leadingContent = {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(profile.color).copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(profile.color))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(profile.name.take(1), fontWeight = FontWeight.Black, color = Color(profile.color))
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text("${profile.batteryLevel}%", style = MaterialTheme.typography.labelSmall)
                Text(profile.transportType ?: "Ghost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
