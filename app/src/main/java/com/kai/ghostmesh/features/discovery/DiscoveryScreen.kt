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
    cornerRadius: Int = 16,
    onNodeClick: (String, String) -> Unit,
    onShout: (String) -> Unit
) {
    var shoutText by remember { mutableStateOf("") }
    var showShoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { contentDescription = "Spectral Radar Status: $meshHealth percent health" }
                    ) {
                        Text("SPECTRAL RADAR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("${connectedNodes.size} NODES ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShoutDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Global Shout" }
                    ) {
                        Icon(Icons.Default.FlashOn, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            RadarView(
                nodes = connectedNodes,
                meshHealth = meshHealth,
                onNodeClick = { id, name -> onNodeClick(id, name) },
                modifier = Modifier
                    .fillMaxSize()
                    .physicalTilt()
                    .semantics { contentDescription = "Interactive radar showing nodes" }
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .physicalTilt(),
                shape = RoundedCornerShape(cornerRadius.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Hub, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("MESH INTEGRITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { meshHealth / 100f },
                            modifier = Modifier.width(120.dp).height(4.dp).clip(CircleShape).semantics { contentDescription = "Mesh Health Progress" },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("$meshHealth%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showShoutDialog) {
        AlertDialog(
            onDismissRequest = { showShoutDialog = false },
            title = { Text("GLOBAL SHOUT") },
            text = {
                OutlinedTextField(
                    value = shoutText,
                    onValueChange = { shoutText = it },
                    placeholder = { Text("Blast a message to all nodes...") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Shout Message Input" }
                )
            },
            confirmButton = {
                Button(
                    onClick = { onShout(shoutText); shoutText = ""; showShoutDialog = false },
                    modifier = Modifier.semantics { contentDescription = "Send Shout" }
                ) {
                    Text("SHOUT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showShoutDialog = false },
                    modifier = Modifier.semantics { contentDescription = "Cancel Shout" }
                ) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }
}
