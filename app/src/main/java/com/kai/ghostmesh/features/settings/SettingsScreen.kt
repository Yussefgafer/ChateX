package com.kai.ghostmesh.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    isDiscoveryEnabled: Boolean,
    isAdvertisingEnabled: Boolean,
    isStealthMode: Boolean,
    isHapticEnabled: Boolean,
    isEncryptionEnabled: Boolean,
    selfDestructSeconds: Int,
    hopLimit: Int,
    packetsSent: Int,
    packetsReceived: Int,
    animationSpeed: Float,
    hapticIntensity: Int,
    messagePreview: Boolean,
    autoReadReceipts: Boolean,
    compactMode: Boolean,
    showTimestamps: Boolean,
    connectionTimeout: Int,
    scanInterval: Long,
    maxImageSize: Int,
    themeMode: Int,
    cornerRadius: Int,
    fontScale: Float,
    isNearbyEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isLanEnabled: Boolean,
    isWifiDirectEnabled: Boolean,
    onProfileChange: (String, String, Int?) -> Unit,
    onToggleDiscovery: (Boolean) -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onToggleStealth: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onSetSelfDestruct: (Int) -> Unit,
    onSetHopLimit: (Int) -> Unit,
    onSetAnimationSpeed: (Float) -> Unit,
    onSetHapticIntensity: (Int) -> Unit,
    onToggleMessagePreview: (Boolean) -> Unit,
    onToggleAutoReadReceipts: (Boolean) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onToggleShowTimestamps: (Boolean) -> Unit,
    onSetConnectionTimeout: (Int) -> Unit,
    onSetScanInterval: (Long) -> Unit,
    onSetMaxImageSize: (Int) -> Unit,
    onSetThemeMode: (Int) -> Unit,
    onSetCornerRadius: (Int) -> Unit,
    onSetFontScale: (Float) -> Unit,
    packetCacheSize: Int,
    onSetPacketCache: (Int) -> Unit,
    onToggleNearby: (Boolean) -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onToggleLan: (Boolean) -> Unit,
    onToggleWifiDirect: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onNavigateToDocs: () -> Unit,
    onBack: () -> Unit
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroup(title = "Profile") {
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it; onProfileChange(it, statusState, null) },
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = statusState,
                    onValueChange = { statusState = it; onProfileChange(nameState, it, null) },
                    label = { Text("Status") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            SettingsGroup(title = "Visuals (God Mode)") {
                ListItem(
                    headlineContent = { Text("Corner Radius: $cornerRadius dp") },
                    supportingContent = {
                        Slider(
                            value = cornerRadius.toFloat(),
                            onValueChange = { onSetCornerRadius(it.toInt()) },
                            valueRange = 0f..32f
                        )
                    },
                    leadingContent = { Icon(Icons.Default.RoundedCorner, null) }
                )
                ListItem(
                    headlineContent = { Text("Font Scale: ${String.format("%.2f", fontScale)}x") },
                    supportingContent = {
                        Slider(
                            value = fontScale,
                            onValueChange = { onSetFontScale(it) },
                            valueRange = 0.8f..1.5f
                        )
                    },
                    leadingContent = { Icon(Icons.Default.TextFields, null) }
                )
            }

            SettingsGroup(title = "Network (Simultaneous)") {
                SettingsToggleItem("Google Nearby (P2P)", Icons.Default.NearbyOff, isNearbyEnabled, onToggleNearby)
                SettingsToggleItem("Bluetooth Legacy", Icons.Default.Bluetooth, isBluetoothEnabled, onToggleBluetooth)
                SettingsToggleItem("LAN (NSD)", Icons.Default.Lan, isLanEnabled, onToggleLan)
                SettingsToggleItem("WiFi Direct", Icons.Default.Wifi, isWifiDirectEnabled, onToggleWifiDirect)

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), thickness = 0.5.dp)

                SettingsToggleItem("Stealth Mode", Icons.Default.VisibilityOff, isStealthMode, onToggleStealth)
                SettingsToggleItem("Auto Discovery", Icons.Default.YoutubeSearchedFor, isDiscoveryEnabled, onToggleDiscovery)
                SettingsToggleItem("Mesh Relay", Icons.Default.Hub, isAdvertisingEnabled, onToggleAdvertising)
                
                ListItem(
                    headlineContent = { Text("Hop Limit: $hopLimit") },
                    supportingContent = {
                        Slider(
                            value = hopLimit.toFloat(),
                            onValueChange = { onSetHopLimit(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Route, null) }
                )

                ListItem(
                    headlineContent = { Text("Connection Timeout: $connectionTimeout s") },
                    supportingContent = {
                        Slider(
                            value = connectionTimeout.toFloat(),
                            onValueChange = { onSetConnectionTimeout(it.toInt()) },
                            valueRange = 5f..120f
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Timer, null) }
                )

                ListItem(
                    headlineContent = { Text("Packet Cache: $packetCacheSize") },
                    supportingContent = {
                        Slider(
                            value = packetCacheSize.toFloat(),
                            onValueChange = { onSetPacketCache(it.toInt()) },
                            valueRange = 500f..5000f,
                            steps = 9
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Storage, null) }
                )
            }

            SettingsGroup(title = "Privacy & Security") {
                SettingsToggleItem("End-to-End Encryption", Icons.Default.Security, isEncryptionEnabled, onToggleEncryption)
                ListItem(
                    headlineContent = { Text("Self-Destruct Timer") },
                    supportingContent = { Text(if (selfDestructSeconds == 0) "Disabled" else "$selfDestructSeconds seconds") },
                    leadingContent = { Icon(Icons.Default.Timer, null) },
                    trailingContent = {
                        TextButton(onClick = { onSetSelfDestruct(if (selfDestructSeconds == 60) 0 else selfDestructSeconds + 10) }) {
                            Text("CHANGE")
                        }
                    }
                )
            }

            SettingsGroup(title = "Help & Support") {
                ListItem(
                    headlineContent = { Text("Knowledge Base") },
                    supportingContent = { Text("Learn about Mesh, Nostr, and Physics") },
                    leadingContent = { Icon(Icons.Default.HelpCenter, null) },
                    modifier = Modifier.clickable { onNavigateToDocs() }
                )
            }

            SettingsGroup(title = "Data Management") {
                ListItem(
                    headlineContent = { Text("Purge All Data") },
                    supportingContent = { Text("Clear all messages and local cache") },
                    leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onClearChat() }
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
