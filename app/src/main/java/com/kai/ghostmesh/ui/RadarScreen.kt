package com.kai.ghostmesh.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.model.UserProfile
import com.kai.ghostmesh.ui.components.MeshNode
import com.kai.ghostmesh.ui.components.MeshRadarBackground
import com.kai.ghostmesh.ui.components.MorphingIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    connectedGhosts: Map<String, UserProfile>,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MeshRadarBackground(pulseColor = MaterialTheme.colorScheme.primary)

        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MorphingIcon(size = 32.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("ChateX Radar", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            // 📶 Mesh Signal Strength Indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (connectedGhosts.isEmpty()) "VOID" else "SPECTRAL LINK: ${connectedGhosts.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connectedGhosts.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
                    contentDescription = null,
                    tint = if (connectedGhosts.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (connectedGhosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MorphingIcon(size = 80.dp, color = Color.Gray.copy(alpha = 0.3f), duration = 4000)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No nodes detected in the void...", color = Color.Gray)
                        }
                    }
                } else {
                    connectedGhosts.entries.forEachIndexed { index, entry ->
                        val profile = entry.value
                        val offsetX = (index % 2) * 180 + 40
                        val offsetY = (index / 2) * 180 + 100
                        MeshNode(
                            name = profile.name,
                            modifier = Modifier.padding(start = offsetX.dp, top = offsetY.dp),
                            onClick = { onNavigateToChat(profile.id, profile.name) }
                        )
                    }
                }
            }
        }

        // Bottom Nav
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = MaterialTheme.shapes.extraLarge,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onNavigateToMessages) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
