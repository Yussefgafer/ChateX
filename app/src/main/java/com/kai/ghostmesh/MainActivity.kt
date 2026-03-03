package com.kai.ghostmesh

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.ui.theme.ChateXTheme
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.messages.MessagesScreen
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsScreen
import com.kai.ghostmesh.features.settings.SettingsViewModel
import com.kai.ghostmesh.features.transfer.TransferHubScreen
import com.kai.ghostmesh.features.transfer.TransferViewModel
import com.kai.ghostmesh.features.docs.DocsScreen
import com.kai.ghostmesh.core.model.UserProfile
import java.util.*

class MainActivity : ComponentActivity() {

    private var meshService: MeshService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MeshService.MeshBinder
            meshService = binder?.getService()
            isServiceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            meshService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startMesh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ChateX)
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        requestIgnoreBatteryOptimizations()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val messagesViewModel: MessagesViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val discoveryViewModel: DiscoveryViewModel = viewModel()
            val transferViewModel: TransferViewModel = viewModel()

            val cornerRadiusState = settingsViewModel.cornerRadius.collectAsState()
            val fontScaleState = settingsViewModel.fontScale.collectAsState()
            val themeModeState = settingsViewModel.themeMode.collectAsState()

            ChateXTheme(
                darkTheme = when(themeModeState.value) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                },
                cornerRadius = cornerRadiusState.value,
                fontScale = fontScaleState.value
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        settingsViewModel, messagesViewModel, chatViewModel,
                        discoveryViewModel, transferViewModel, cornerRadiusState.value, fontScaleState.value, themeModeState.value
                    )
                }
            }
        }
    }

    @Composable
    private fun AppNavigation(
        settingsViewModel: SettingsViewModel,
        messagesViewModel: MessagesViewModel,
        chatViewModel: ChatViewModel,
        discoveryViewModel: DiscoveryViewModel,
        transferViewModel: TransferViewModel,
        cornerRadiusSetting: Int,
        fontScaleSetting: Float,
        themeModeValue: Int
    ) {
        val navController = rememberNavController()
        
        val userProfileState = settingsViewModel.userProfile.collectAsState()
        val mnemonicState = settingsViewModel.mnemonic.collectAsState()
        val recentChatsState = messagesViewModel.recentChats.collectAsState(initial = emptyList())
        val onlinePeersState = messagesViewModel.onlinePeers.collectAsState()
        val meshHealthState = messagesViewModel.meshHealth.collectAsState()
        val chatHistoryState = chatViewModel.messages.collectAsState(initial = emptyList())
        val typingPeersState = chatViewModel.typingPeers.collectAsState()
        val replyToMessageState = chatViewModel.replyToMessage.collectAsState()
        val activeTransfersState = transferViewModel.activeTransfers.collectAsState()

        val stealthModeState = settingsViewModel.isStealthMode.collectAsState()
        val encryptionEnabledState = settingsViewModel.isEncryptionEnabled.collectAsState()
        val isNearbyEnabledState = settingsViewModel.isNearbyEnabled.collectAsState()
        val isBluetoothEnabledState = settingsViewModel.isBluetoothEnabled.collectAsState()
        val isLanEnabledState = settingsViewModel.isLanEnabled.collectAsState()
        val isWifiDirectEnabledState = settingsViewModel.isWifiDirectEnabled.collectAsState()

        val hopLimitState = settingsViewModel.hopLimit.collectAsState()
        val selfDestructState = settingsViewModel.selfDestructSeconds.collectAsState()

        NavHost(
            navController = navController,
            startDestination = "messages",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
        ) {
            composable("messages") {
                MessagesScreen(
                    chats = recentChatsState.value,
                    cornerRadius = cornerRadiusSetting,
                    onNavigateToChat = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onNavigateToRadar = { navController.navigate("discovery") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onRefresh = { messagesViewModel.refreshConnections() }
                )
            }
            composable("discovery") {
                DiscoveryScreen(
                    connectedNodes = onlinePeersState.value,
                    meshHealth = meshHealthState.value,
                    cornerRadius = cornerRadiusSetting,
                    onNodeClick = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onShout = { discoveryViewModel.globalShout(it, encryptionEnabledState.value, hopLimitState.value, userProfileState.value) }
                )
            }
            composable("chat/{peerId}/{peerName}", arguments = listOf(navArgument("peerId") { type = NavType.StringType }, navArgument("peerName") { type = NavType.StringType })) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"
                val ghostProfile = onlinePeersState.value[peerId]

                ChatScreen(
                    peerId = peerId, peerName = peerName, messages = chatHistoryState.value,
                    isTyping = typingPeersState.value.contains(peerId),
                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabledState.value, selfDestructState.value, hopLimitState.value, userProfileState.value) },
                    onSendImage = { uri: Uri -> chatViewModel.sendImage(uri, encryptionEnabledState.value, selfDestructState.value, hopLimitState.value, userProfileState.value) },
                    onSendVideo = { uri: Uri -> chatViewModel.sendVideo(uri, encryptionEnabledState.value, selfDestructState.value, hopLimitState.value, userProfileState.value) },
                    onStartVoice = { chatViewModel.startRecording() },
                    onStopVoice = { chatViewModel.stopRecording() },
                    onPlayVoice = { chatViewModel.playVoice(it) },
                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                    onTypingChange = { chatViewModel.sendTyping(it, userProfileState.value) },
                    onBack = { navController.popBackStack() },
                    replyToMessage = replyToMessageState.value,
                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                    onClearReply = { chatViewModel.clearReply() },
                    cornerRadius = cornerRadiusSetting,
                    transportType = ghostProfile?.transportType
                )
            }
            composable("settings") {
                SettingsScreen(
                    profile = userProfileState.value,
                    isDiscoveryEnabled = settingsViewModel.isDiscoveryEnabled.collectAsState().value,
                    isAdvertisingEnabled = settingsViewModel.isAdvertisingEnabled.collectAsState().value,
                    isStealthMode = stealthModeState.value,
                    isHapticEnabled = settingsViewModel.isHapticEnabled.collectAsState().value,
                    isEncryptionEnabled = encryptionEnabledState.value,
                    selfDestructSeconds = selfDestructState.value,
                    hopLimit = hopLimitState.value,
                    packetsSent = settingsViewModel.packetsSent.collectAsState().value,
                    packetsReceived = settingsViewModel.packetsReceived.collectAsState().value,
                    animationSpeed = settingsViewModel.animationSpeed.collectAsState().value,
                    hapticIntensity = settingsViewModel.hapticIntensity.collectAsState().value,
                    messagePreview = settingsViewModel.messagePreview.collectAsState().value,
                    autoReadReceipts = settingsViewModel.autoReadReceipts.collectAsState().value,
                    compactMode = settingsViewModel.compactMode.collectAsState().value,
                    showTimestamps = settingsViewModel.showTimestamps.collectAsState().value,
                    connectionTimeout = settingsViewModel.connectionTimeout.collectAsState().value,
                    scanInterval = settingsViewModel.scanInterval.collectAsState().value,
                    maxImageSize = settingsViewModel.maxImageSize.collectAsState().value,
                    themeMode = themeModeValue,
                    cornerRadius = cornerRadiusSetting, fontScale = fontScaleSetting,
                    isNearbyEnabled = isNearbyEnabledState.value, isBluetoothEnabled = isBluetoothEnabledState.value,
                    isLanEnabled = isLanEnabledState.value, isWifiDirectEnabled = isWifiDirectEnabledState.value,
                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                    onToggleDiscovery = { settingsViewModel.updateSetting("discovery", it) },
                    onToggleAdvertising = { settingsViewModel.updateSetting("advertising", it) },
                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                    onToggleHaptic = { settingsViewModel.updateSetting("haptic", it) },
                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                    onSetSelfDestruct = { settingsViewModel.updateSetting("self_destruct", it) },
                    onSetHopLimit = { settingsViewModel.updateSetting(AppConfig.KEY_HOP_LIMIT, it) },
                    onSetAnimationSpeed = { settingsViewModel.updateSetting("animation_speed", it) },
                    onSetHapticIntensity = { settingsViewModel.updateSetting("haptic_intensity", it) },
                    onToggleMessagePreview = { settingsViewModel.updateSetting("message_preview", it) },
                    onToggleAutoReadReceipts = { settingsViewModel.updateSetting("auto_read_receipts", it) },
                    onToggleCompactMode = { settingsViewModel.updateSetting("compact_mode", it) },
                    onToggleShowTimestamps = { settingsViewModel.updateSetting("show_timestamps", it) },
                    onSetConnectionTimeout = { settingsViewModel.updateSetting(AppConfig.KEY_CONN_TIMEOUT, it) },
                    onSetScanInterval = { settingsViewModel.updateSetting(AppConfig.KEY_SCAN_INTERVAL, it) },
                    onSetMaxImageSize = { settingsViewModel.updateSetting("max_image_size", it) },
                    onSetThemeMode = { settingsViewModel.updateSetting("theme_mode", it) },
                    onSetCornerRadius = { settingsViewModel.updateSetting(AppConfig.KEY_CORNER_RADIUS, it) },
                    onSetFontScale = { settingsViewModel.updateSetting(AppConfig.KEY_FONT_SCALE, it) },
                    packetCacheSize = 150,
                    onSetPacketCache = { },
                    onToggleNearby = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_NEARBY, it) },
                    onToggleBluetooth = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                    onToggleLan = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_LAN, it) },
                    onToggleWifiDirect = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                    onClearChat = { settingsViewModel.clearHistory() },
                    onNavigateToDocs = { navController.navigate("docs") },
                    onBack = { navController.popBackStack() },
                    onNavigateToTransfers = { navController.navigate("transfer_hub") },
                    mnemonic = mnemonicState.value,
                    onGenerateBackup = { settingsViewModel.generateBackupMnemonic() },
                    onRestoreIdentity = { settingsViewModel.restoreIdentity(it) { success ->
                        if (success) Toast.makeText(this@MainActivity, "Identity Restored", Toast.LENGTH_LONG).show()
                    }}
                )
            }
            composable("transfer_hub") {
                TransferHubScreen(
                    transfers = activeTransfersState.value,
                    onCancel = { transferViewModel.cancelTransfer(it) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("docs") {
                DocsScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.POST_NOTIFICATIONS))
        }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startMesh() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startMesh() {
        if (isServiceBound) return
        val prefs = getSharedPreferences(com.kai.ghostmesh.core.model.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val intent = Intent(this, MeshService::class.java).apply {
            putExtra("NICKNAME", prefs.getString("nick", "Peer"))
            putExtra("STEALTH", prefs.getBoolean("stealth", false))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }
}
