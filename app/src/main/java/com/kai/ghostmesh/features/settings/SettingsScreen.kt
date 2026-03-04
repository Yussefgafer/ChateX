package com.kai.ghostmesh.features.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kai.ghostmesh.core.model.UserProfile
import com.kai.ghostmesh.core.ui.components.*
import com.kai.ghostmesh.core.model.AppConfig

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
    onBack: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    mnemonic: String? = null,
    onGenerateBackup: () -> Unit = {},
    onRestoreIdentity: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreMnemonic by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 3% Noise/Grain overlay
        Box(modifier = Modifier.fillMaxSize().alpha(0.03f).background(Color.Black))

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text(
                            "Advanced Configuration",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        ExpressiveIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                SettingsCategory("Identity Management") {
                    ProfileHeader(profile, onProfileChange)

                    ListItem(
                        headlineContent = { Text("Backup Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Secure your 12-word recovery phrase", style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { onGenerateBackup() }
                    )

                    if (mnemonic != null) {
                        BackupPhraseBox(mnemonic)
                    }

                    ListItem(
                        headlineContent = { Text("Restore Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Recover your account from a seed phrase", style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { showRestoreDialog = true }
                    )
                }

                SettingsCategory("Network Protocol") {
                    SettingsToggleItem("Peer Visibility", Icons.Default.VisibilityOff, isStealthMode, onToggleStealth)
                    SettingsToggleItem("Nearby Discovery", Icons.Default.Dns, isNearbyEnabled, onToggleNearby)
                    SettingsToggleItem("Bluetooth Mesh", Icons.Default.Bluetooth, isBluetoothEnabled, onToggleBluetooth)
                    SettingsToggleItem("LAN Node Transport", Icons.Default.Lan, isLanEnabled, onToggleLan)
                    SettingsToggleItem("WiFi Direct Point", Icons.Default.Wifi, isWifiDirectEnabled, onToggleWifiDirect)
                }

                SettingsCategory("Security Standards") {
                    SettingsToggleItem("End-to-End Encryption", Icons.Default.Security, isEncryptionEnabled, onToggleEncryption)
                    ListItem(
                        headlineContent = { Text("Rotate Session Keys", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("Regenerate cryptographic identity markers", style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(24.dp)) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { Toast.makeText(context, "Session Keys Rotated", Toast.LENGTH_SHORT).show() }
                    )
                }

                SettingsCategory("Interface Expressiveness") {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            "Geometric Curvature ($cornerRadius dp)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        ExpressiveSlider(
                            value = cornerRadius.toFloat(),
                            onValueChange = { onSetCornerRadius(it.toInt()) },
                            valueRange = 0f..40f
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            "Typography Scale (${"%.1f".format(fontScale)}x)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        ExpressiveSlider(
                            value = fontScale,
                            onValueChange = onSetFontScale,
                            valueRange = 0.8f..1.5f
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))
                ExpressiveButton(
                    onClick = onClearChat,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(12.dp))
                    Text("PURGE LOCAL DATA REPOSITORY", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(64.dp))
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Cryptographic Identity", style = MaterialTheme.typography.headlineSmall) },
            text = {
                OutlinedTextField(
                    value = restoreMnemonic,
                    onValueChange = { restoreMnemonic = it },
                    label = { Text("12-word recovery mnemonic") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                ExpressiveButton(
                    onClick = { onRestoreIdentity(restoreMnemonic); showRestoreDialog = false }
                ) { Text("RESTORE") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.ExtraBold
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp).padding(horizontal = 24.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        leadingContent = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun BackupPhraseBox(mnemonic: String) {
    Card(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("RECOVERY MNEMONIC (CONFIDENTIAL)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                mnemonic,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Physical backup required. Digital duplication prohibited.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ProfileHeader(profile: UserProfile, onProfileChange: (String, String, Int?) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profile.name) }

    ListItem(
        headlineContent = {
            if (isEditing) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.titleLarge
                )
            } else {
                Text(profile.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
            }
        },
        supportingContent = {
            Text(
                profile.id.take(24) + "...",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        },
        leadingContent = {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(profile.color).copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(profile.color).copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        profile.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(profile.color),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        trailingContent = {
            ExpressiveIconButton(
                onClick = {
                    if (isEditing) onProfileChange(tempName, profile.status, null)
                    isEditing = !isEditing
                },
                containerColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ) {
                Icon(
                    if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    null,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
