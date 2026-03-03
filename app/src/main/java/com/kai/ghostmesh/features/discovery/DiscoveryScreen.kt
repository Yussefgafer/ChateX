package com.kai.ghostmesh.features.discovery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
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
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            FloatingActionButton(
                onClick = { showShoutDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                interactionSource = interactionSource,
                shape = remember {
                    object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(
                            size: androidx.compose.ui.geometry.Size,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            density: androidx.compose.ui.unit.Density
                        ): androidx.compose.ui.graphics.Outline {
                            val polygon = RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.92f, rounding = androidx.graphics.shapes.CornerRounding(0.2f))
                            val path = polygon.toPath().asComposePath()
                            val matrix = android.graphics.Matrix()
                            val scale = size.minDimension / 2f
                            matrix.setScale(scale, scale)
                            matrix.postTranslate(size.width / 2f, size.height / 2f)
                            path.asAndroidPath().transform(matrix)
                            return androidx.compose.ui.graphics.Outline.Generic(path)
                        }
                    }
                },
                modifier = Modifier.graphicsLayer {
                    val scale = if (isPressed) 0.88f else 1f
                    scaleX = scale
                    scaleY = scale
                }
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
                        ExpressiveButton(
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
                        ExpressiveButton(
                            onClick = { showShoutDialog = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { contentDescription = "Cancel Shout" }
                        ) { Text("CANCEL") }
                    }
                )
            }
        }
    }
}
