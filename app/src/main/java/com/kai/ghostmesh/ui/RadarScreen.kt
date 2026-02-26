package com.kai.ghostmesh.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.model.UserProfile
import com.kai.ghostmesh.ui.components.MeshNode
import com.kai.ghostmesh.ui.components.MeshRadarBackground
import com.kai.ghostmesh.ui.components.MorphingIcon
import com.kai.ghostmesh.ui.components.spectralGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    connectedGhosts: Map<String, UserProfile>,
    connectionQuality: Int = 100,
    onGlobalShout: (String) -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showShoutDialog by remember { mutableStateOf(false) }
    var shoutText by remember { mutableStateOf("") }
    
    val qualityColor = when {
        connectionQuality >= 75 -> Color(0xFF00FF7F)
        connectionQuality >= 50 -> Color(0xFFFFD700)
        connectionQuality > 0 -> Color(0xFFFF3131)
        else -> Color.Gray
    }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) { 
                            Icon(Icons.Default.Radar, "Radar", tint = Color.Black) 
                        }
                        IconButton(onClick = onNavigateToMessages) { Icon(Icons.Default.ChatBubble, "Archives", tint = Color.Gray) }
                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.Gray) }
                    }
                }
            }
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showShoutDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MeshRadarBackground(pulseColor = MaterialTheme.colorScheme.primary)

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Q: $connectionQuality%", style = MaterialTheme.typography.labelSmall, color = qualityColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(color = qualityColor.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (connectedGhosts.isEmpty()) "SEARCHING..." else "LINKED: ${connectedGhosts.size}", style = MaterialTheme.typography.labelSmall, color = if (connectedGhosts.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.SignalWifi4Bar, null, modifier = Modifier.size(14.dp), tint = if (connectedGhosts.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary)
                }
            }

            if (connectedGhosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorphingIcon(size = 100.dp, color = Color.Gray.copy(alpha = 0.2f), duration = 5000)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Void Scanning...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 120.dp), contentPadding = PaddingValues(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(connectedGhosts.values.toList(), key = { it.id }) { profile ->
                        MeshNode(name = profile.name, modifier = Modifier.spectralGlow(Color(profile.color), radius = 8.dp), onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToChat(profile.id, profile.name) })
                    }
                }
            }
        }

        if (showShoutDialog) {
            AlertDialog(
                onDismissRequest = { showShoutDialog = false },
                title = { Text("Broadcast to the Void") },
                text = {
                    OutlinedTextField(
                        value = shoutText,
                        onValueChange = { shoutText = it },
                        placeholder = { Text("Everyone will hear this...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { if (shoutText.isNotBlank()) { onGlobalShout(shoutText); shoutText = ""; showShoutDialog = false } }) { Text("SHOUT") }
                },
                dismissButton = {
                    TextButton(onClick = { showShoutDialog = false }) { Text("CANCEL") }
                }
            )
        }
    }
}
