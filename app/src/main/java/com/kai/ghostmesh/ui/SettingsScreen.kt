package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    isDiscoveryEnabled: Boolean,
    isAdvertisingEnabled: Boolean,
    isHapticEnabled: Boolean,
    onProfileChange: (String, String) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("ChateX Console", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // --- SECTION: Spectral Identity ---
                SettingsSection(title = "Spectral Identity", icon = Icons.Default.Person) {
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { 
                            nameState = it
                            onProfileChange(it, statusState)
                        },
                        label = { Text("Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ghostTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = statusState,
                        onValueChange = { 
                            statusState = it
                            onProfileChange(nameState, it)
                        },
                        label = { Text("Spectral Status") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ghostTextFieldColors()
                    )
                }

                // --- SECTION: Mesh Control ---
                SettingsSection(title = "Mesh Control", icon = Icons.Default.SettingsInputAntenna) {
                    SettingsSwitch(
                        label = "Nearby Discovery",
                        description = "Look for other ghosts in the void",
                        checked = isDiscoveryEnabled,
                        onCheckedChange = onToggleDiscovery
                    )
                    SettingsSwitch(
                        label = "Spectral Advertising",
                        description = "Let others find your presence",
                        checked = isAdvertisingEnabled,
                        onCheckedChange = onToggleAdvertising
                    )
                }

                // --- SECTION: Experience ---
                SettingsSection(title = "Experience", icon = Icons.Default.AutoFixHigh) {
                    SettingsSwitch(
                        label = "Haptic Feedback",
                        description = "Feel the mesh vibrations",
                        checked = isHapticEnabled,
                        onCheckedChange = onToggleHaptic
                    )
                    
                    Button(
                        onClick = onClearChat,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge Chat History")
                    }
                }

                // --- SECTION: About ---
                SettingsSection(title = "System Info", icon = Icons.Default.Info) {
                    Text("Version: 1.2.0-Spectral", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("Engine: ChateX Mesh Engine 2026", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Architect: Jo & Kai", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitch(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(description, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ghostTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
