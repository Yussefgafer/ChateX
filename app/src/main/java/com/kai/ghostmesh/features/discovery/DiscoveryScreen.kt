package com.kai.ghostmesh.features.discovery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscoveryScreen(
    connectedNodes: Map<String, UserProfile>,
    meshHealth: Int,
    cornerRadius: Int,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var showShoutDialog by remember { mutableStateOf(false) }
    var shoutText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SPECTRAL RADAR",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = "Spectral Radar Screen" }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { contentDescription = "${connectedNodes.size} nodes connected" }
                    ) {
                        Text("${connectedNodes.size} NODES")
                    }
                    Spacer(Modifier.width(16.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showShoutDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Default.BroadcastOnPersonal, contentDescription = "Global Shout")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            RadarView(
                nodes = connectedNodes,
                meshHealth = meshHealth,
                onNodeClick = onNodeClick,
                modifier = Modifier.fillMaxSize()
            )

            if (showShoutDialog) {
                AlertDialog(
                    onDismissRequest = { showShoutDialog = false },
                    title = { Text("Global Transmission") },
                    text = {
                        OutlinedTextField(
                            value = shoutText,
                            onValueChange = { shoutText = it },
                            placeholder = { Text("Broadcast to the entire mesh...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (shoutText.isNotBlank()) {
                                    onShout(shoutText)
                                    shoutText = ""
                                    showShoutDialog = false
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = "Confirm Shout" }
                        ) { Text("SHOUT") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showShoutDialog = false },
                            modifier = Modifier.semantics { contentDescription = "Cancel Shout" }
                        ) { Text("CANCEL") }
                    }
                )
            }
        }
    }
}
