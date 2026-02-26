package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isEncryptionEnabled: Boolean,
    selfDestructSeconds: Int,
    onProfileChange: (String, String) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onSetSelfDestruct: (Int) -> Unit,
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
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
                // Identity
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
                        label = { Text("Status") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ghostTextFieldColors()
                    )
                }

                // Privacy
                SettingsSection(title = "Ghostly Effects", icon = Icons.Default.LocalFireDepartment) {
                    Text("Burn After Reading", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("Messages will fade from existence", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(0, 10, 30, 60).forEach { sec ->
                            FilterChip(
                                selected = selfDestructSeconds == sec,
                                onClick = { onSetSelfDestruct(sec) },
                                label = { Text(if (sec == 0) "Off" else "${sec}s") }
                            )
                        }
                    }
                }

                // Security
                SettingsSection(title = "Security", icon = Icons.Default.Security) {
                    SettingsSwitch(
                        label = "Spectral Encryption",
                        description = "End-to-End AES protection",
                        checked = isEncryptionEnabled,
                        onCheckedChange = onToggleEncryption
                    )
                }

                // Mesh Control
                SettingsSection(title = "Mesh Control", icon = Icons.Default.SettingsInputAntenna) {
                    SettingsSwitch(label = "Nearby Discovery", checked = isDiscoveryEnabled, onCheckedChange = onToggleDiscovery)
                    SettingsSwitch(label = "Spectral Advertising", checked = isAdvertisingEnabled, onCheckedChange = onToggleAdvertising)
                }

                // Experience
                SettingsSection(title = "Experience", icon = Icons.Default.Palette) {
                    SettingsSwitch(label = "Tactile Mesh (Haptics)", checked = isHapticEnabled, onCheckedChange = onToggleHaptic)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClearChat,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge Archives")
                    }
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
            Column(modifier = Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
fun SettingsSwitch(label: String, description: String = "", checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotBlank()) Text(description, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ghostTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
