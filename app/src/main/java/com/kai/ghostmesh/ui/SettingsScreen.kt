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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChateX Console", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(selected = false, onClick = onBack, icon = { Icon(Icons.Default.Radar, null) }, label = { Text("Radar") })
                NavigationBarItem(selected = false, onClick = onBack, icon = { Icon(Icons.Default.ChatBubble, null) }, label = { Text("Messages") })
                NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section: Profile
            SettingsHeader("Spectral Identity")
            OutlinedTextField(
                value = nameState,
                onValueChange = { nameState = it; onProfileChange(it, statusState) },
                label = { Text("Ghost Nickname") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = statusState,
                onValueChange = { statusState = it; onProfileChange(nameState, it) },
                label = { Text("Spectral Status") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Mesh Engine
            SettingsHeader("Mesh Engine")
            SettingsListItem(
                title = "Search for Nearby Signals",
                subtitle = "Look for other ChateX nodes",
                icon = Icons.Default.Radar,
                trailing = { Switch(checked = isDiscoveryEnabled, onCheckedChange = onToggleDiscovery) }
            )
            SettingsListItem(
                title = "Spectral Presence",
                subtitle = "Broadcast your ID to others",
                icon = Icons.Default.Wifi,
                trailing = { Switch(checked = isAdvertisingEnabled, onCheckedChange = onToggleAdvertising) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Security
            SettingsHeader("Security & Void")
            SettingsListItem(
                title = "End-to-End Encryption",
                subtitle = "AES-256 protected packets",
                icon = Icons.Default.Security,
                trailing = { Switch(checked = isEncryptionEnabled, onCheckedChange = onToggleEncryption) }
            )
            
            ListItem(
                headlineContent = { Text("Burn After Reading") },
                supportingContent = {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 10, 30, 60).forEach { sec ->
                            FilterChip(
                                selected = selfDestructSeconds == sec,
                                onClick = { onSetSelfDestruct(sec) },
                                label = { Text(if (sec == 0) "Off" else "${sec}s") }
                            )
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Red) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Danger Zone
            SettingsHeader("Void Maintenance")
            Button(
                onClick = onClearChat,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Purge All Spectral Archives")
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun SettingsListItem(title: String, subtitle: String, icon: ImageVector, trailing: @Composable () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
