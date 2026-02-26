package com.kai.ghostmesh.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MorphingIcon(size = 32.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("ChateX Radar", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Radar, contentDescription = null) },
                    label = { Text("Radar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMessages,
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = null) },
                    label = { Text("Messages") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // 1. Performance-optimized background
            MeshRadarBackground(pulseColor = MaterialTheme.colorScheme.primary)

            // 2. The Grid of Nodes (Scalable UI)
            if (connectedGhosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorphingIcon(size = 100.dp, color = Color.Gray.copy(alpha = 0.2f), duration = 5000)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Searching for signals...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        Text("Ensure others have ChateX open", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            } else {
                // Using Grid for better organization than random offsets
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(connectedGhosts.values.toList()) { profile ->
                        MeshNode(
                            name = profile.name,
                            onClick = { onNavigateToChat(profile.id, profile.name) }
                        )
                    }
                }
            }
        }
    }
}
