package com.kai.ghostmesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    isEncryptionEnabled: Boolean,
    selfDestructSeconds: Int,
    hopLimit: Int,
    onProfileChange: (String, String) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onSetSelfDestruct: (Int) -> Unit,
    onSetHopLimit: (Int) -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChateX Console") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            SettingsHeader("Spectral Identity")
            OutlinedTextField(value = nameState, onValueChange = { nameState = it; onProfileChange(it, statusState) }, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = statusState, onValueChange = { statusState = it; onProfileChange(nameState, it) }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Mesh Parameters")
            SettingsListItem(title = "Max Hop Count", subtitle = "Packet range: $hopLimit nodes", icon = Icons.Default.Route) {
                Slider(value = hopLimit.toFloat(), onValueChange = { onSetHopLimit(it.toInt()) }, valueRange = 1f..5f, steps = 3, modifier = Modifier.width(100.dp))
            }
            SettingsListItem(title = "Discovery", icon = Icons.Default.Radar, checked = isDiscoveryEnabled, onCheckedChange = onToggleDiscovery)
            SettingsListItem(title = "Advertising", icon = Icons.Default.Wifi, checked = isAdvertisingEnabled, onCheckedChange = onToggleAdvertising)

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Security")
            SettingsListItem(title = "E2EE Encryption", icon = Icons.Default.Security, checked = isEncryptionEnabled, onCheckedChange = onToggleEncryption)
            
            ListItem(
                headlineContent = { Text("Self-Destruct Timer") },
                supportingContent = {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 10, 30, 60).forEach { sec ->
                            FilterChip(selected = selfDestructSeconds == sec, onClick = { onSetSelfDestruct(sec) }, label = { Text(if (sec == 0) "Off" else "${sec}s") })
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color.Red) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsHeader("Maintenance")
            Button(onClick = onClearChat, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Purge Records")
            }
        }
    }
}

@Composable
fun SettingsHeader(text: String) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun SettingsListItem(title: String, icon: ImageVector, subtitle: String? = null, checked: Boolean? = null, onCheckedChange: ((Boolean) -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        leadingContent = { Icon(icon, null) },
        trailingContent = when {
            checked != null -> { { Switch(checked = checked, onCheckedChange = onCheckedChange!!) } }
            trailing != null -> trailing
            else -> null
        }
    )
}
