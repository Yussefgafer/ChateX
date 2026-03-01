package com.kai.ghostmesh.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.security.SecurityManager
import java.io.File

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
    onBack: () -> Unit,
    onSetProfileImage: (Uri) -> Unit = {},
    onNavigateToDocs: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onRestoreIdentity: (String) -> Unit = {}
) {
    var nameState by remember { mutableStateOf(profile.name) }
    var statusState by remember { mutableStateOf(profile.status) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreSeedText by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { onSetProfileImage(it) } }
    )

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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val imagePath = profile.profileImageLocalPath
                        if (imagePath != null && File(imagePath).exists()) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nameState,
                            onValueChange = { nameState = it; onProfileChange(it, statusState, null) },
                            label = { Text("Nickname") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = statusState,
                            onValueChange = { statusState = it; onProfileChange(nameState, it, null) },
                            label = { Text("Status") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            SettingsGroup(title = "Mission Assets") {
                ListItem(
                    headlineContent = { Text("Offline Void Map") },
                    supportingContent = { Text("View discovered ghosts on a local map") },
                    leadingContent = { Icon(Icons.Default.Map, null) },
                    modifier = Modifier.clickable { onNavigateToMaps() }
                )
                ListItem(
                    headlineContent = { Text("Knowledge Base") },
                    supportingContent = { Text("Protocol documentation and help") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                    modifier = Modifier.clickable { onNavigateToDocs() }
                )
            }

            SettingsGroup(title = "Identity & Security") {
                ListItem(
                    headlineContent = { Text("Backup Identity") },
                    supportingContent = { Text("View your 12-word seed phrase") },
                    leadingContent = { Icon(Icons.Default.VpnKey, null) },
                    modifier = Modifier.clickable { showSeedDialog = true }
                )
                ListItem(
                    headlineContent = { Text("Restore Identity") },
                    supportingContent = { Text("Recover an existing Ghost profile") },
                    leadingContent = { Icon(Icons.Default.SettingsBackupRestore, null) },
                    modifier = Modifier.clickable { showRestoreDialog = true }
                )
                SettingsToggleItem("End-to-End Encryption", Icons.Default.Security, isEncryptionEnabled, onToggleEncryption)
            }

            SettingsGroup(title = "Network (Simultaneous)") {
                SettingsToggleItem("Google Nearby (P2P)", Icons.Default.NearbyOff, isNearbyEnabled, onToggleNearby)
                SettingsToggleItem("Bluetooth Legacy", Icons.Default.Bluetooth, isBluetoothEnabled, onToggleBluetooth)
                SettingsToggleItem("LAN (NSD)", Icons.Default.Lan, isLanEnabled, onToggleLan)
                SettingsToggleItem("WiFi Direct", Icons.Default.Wifi, isWifiDirectEnabled, onToggleWifiDirect)
            }

            ListItem(
                headlineContent = { Text("Advanced Settings", fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(Icons.Default.SettingsSuggest, null, tint = MaterialTheme.colorScheme.secondary) },
                trailingContent = {
                    Switch(checked = showAdvanced, onCheckedChange = { showAdvanced = it })
                },
                modifier = Modifier.clickable { showAdvanced = !showAdvanced }
            )

            if (showAdvanced) {
                SettingsGroup(title = "Mission Hardening") {
                    SettingsToggleItem("Master Election", Icons.Default.AdminPanelSettings, true) { }
                    ListItem(
                        headlineContent = { Text("Heartbeat Interval: 5s") },
                        supportingContent = { Slider(value = 0.5f, onValueChange = {}) },
                        leadingContent = { Icon(Icons.Default.MonitorHeart, null) }
                    )
                }
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

    if (showSeedDialog) {
        val seed = SecurityManager.getSeedPhrase()?.joinToString(" ") ?: "Generating..."
        AlertDialog(
            onDismissRequest = { showSeedDialog = false },
            title = { Text("YOUR IDENTITY SEED") },
            text = {
                Column {
                    Text("KEEP THIS PRIVATE. These 12 words can recover your entire ChateX identity.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(seed, modifier = Modifier.padding(16.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSeedDialog = false }) { Text("I HAVE SAVED IT") }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("RESTORE IDENTITY") },
            text = {
                Column {
                    Text("Enter your 12-word seed phrase below. This will replace your current identity.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreSeedText,
                        onValueChange = { restoreSeedText = it },
                        placeholder = { Text("word1 word2 ...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRestoreIdentity(restoreSeedText)
                    showRestoreDialog = false
                }) { Text("RESTORE") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
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
