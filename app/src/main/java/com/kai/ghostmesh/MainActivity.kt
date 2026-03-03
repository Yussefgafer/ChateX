package com.kai.ghostmesh

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kai.ghostmesh.core.model.AppConfig
import com.kai.ghostmesh.core.ui.theme.ChateXTheme
import com.kai.ghostmesh.features.chat.ChatScreen
import com.kai.ghostmesh.features.chat.ChatViewModel
import com.kai.ghostmesh.features.discovery.DiscoveryScreen
import com.kai.ghostmesh.features.discovery.DiscoveryViewModel
import com.kai.ghostmesh.features.docs.DocsScreen
import com.kai.ghostmesh.features.messages.MessagesScreen
import com.kai.ghostmesh.features.messages.MessagesViewModel
import com.kai.ghostmesh.features.settings.SettingsScreen
import com.kai.ghostmesh.features.settings.SettingsViewModel
import com.kai.ghostmesh.features.transfer.TransferHubScreen
import com.kai.ghostmesh.features.transfer.TransferViewModel
import com.kai.ghostmesh.service.MeshService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) startMesh()
    }

    private var meshService: MeshService? = null
    private val isServiceReady = mutableStateOf(false)
    private var isServiceBound = false

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as? MeshService.MeshBinder
            meshService = binder?.getService()
            isServiceBound = true
            meshService?.let {
                lifecycleScope.launch {
                    it.isReady.collectLatest { ready ->
                        isServiceReady.value = ready
                    }
                }
            }
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            meshService = null
            isServiceReady.value = false
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ChateX)
        super.onCreate(savedInstanceState)
        
        requestIgnoreBatteryOptimizations()
        checkAndRequestPermissions()

        setContent {
            val messagesViewModel: MessagesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val discoveryViewModel: DiscoveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val transferViewModel: TransferViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val cornerRadius by settingsViewModel.cornerRadius.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val ready by isServiceReady

            ChateXTheme(
                darkTheme = when(themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() },
                cornerRadius = cornerRadius,
                fontScale = fontScale
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val navController = rememberNavController()

                LaunchedEffect(chatViewModel.error) {
                    chatViewModel.error.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
                }
                LaunchedEffect(discoveryViewModel.error) {
                    discoveryViewModel.error.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
                }

                if (!ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Initializing Mesh Service...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Scaffold { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            MainContent(
                                messagesViewModel, chatViewModel, discoveryViewModel, settingsViewModel,
                                transferViewModel,
                                navController
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        messagesViewModel: MessagesViewModel,
        chatViewModel: ChatViewModel,
        discoveryViewModel: DiscoveryViewModel,
        settingsViewModel: SettingsViewModel,
        transferViewModel: TransferViewModel,
        navController: androidx.navigation.NavHostController
    ) {
        val recentChats by messagesViewModel.recentChats.collectAsState()
        
        val chatHistory by chatViewModel.messages.collectAsState()
        val typingPeersState = chatViewModel.typingPeers.collectAsState(initial = emptySet())
        val typingPeers = typingPeersState.value
        val replyToMessage by chatViewModel.replyToMessage.collectAsState()

        val onlinePeers by discoveryViewModel.connectedNodes.collectAsState()
        val meshHealth by discoveryViewModel.meshHealth.collectAsState()

        val userProfile by settingsViewModel.userProfile.collectAsState()
        val discoveryEnabled by settingsViewModel.isDiscoveryEnabled.collectAsState()
        val advertisingEnabled by settingsViewModel.isAdvertisingEnabled.collectAsState()
        val stealthMode by settingsViewModel.isStealthMode.collectAsState()
        val hapticEnabled by settingsViewModel.isHapticEnabled.collectAsState()
        val encryptionEnabled by settingsViewModel.isEncryptionEnabled.collectAsState()
        val selfDestructSeconds by settingsViewModel.selfDestructSeconds.collectAsState()
        val hopLimit by settingsViewModel.hopLimit.collectAsState()
        val animationSpeed by settingsViewModel.animationSpeed.collectAsState()
        val hapticIntensity by settingsViewModel.hapticIntensity.collectAsState()
        val messagePreview by settingsViewModel.messagePreview.collectAsState()
        val autoReadReceipts by settingsViewModel.autoReadReceipts.collectAsState()
        val compactMode by settingsViewModel.compactMode.collectAsState()
        val showTimestamps by settingsViewModel.showTimestamps.collectAsState()
        val connectionTimeout by settingsViewModel.connectionTimeout.collectAsState()
        val scanInterval by settingsViewModel.scanInterval.collectAsState()
        val maxImageSize by settingsViewModel.maxImageSize.collectAsState()
        val themeMode by settingsViewModel.themeMode.collectAsState()
        val cornerRadiusSetting by settingsViewModel.cornerRadius.collectAsState()
        val fontScaleSetting by settingsViewModel.fontScale.collectAsState()
        val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
        val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
        val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
        val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()

        val packetsSent by settingsViewModel.packetsSent.collectAsState()
        val packetsReceived by settingsViewModel.packetsReceived.collectAsState()

        val activeTransfers by transferViewModel.transfers.collectAsState()

        NavHost(
            navController = navController,
            startDestination = "messages",
            enterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            exitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popEnterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popExitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
        ) {
            composable("messages") {
                MessagesScreen(
                    chats = recentChats,
                    cornerRadius = cornerRadiusSetting,
                    onNavigateToChat = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onNavigateToRadar = { navController.navigate("discovery") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onRefresh = { messagesViewModel.refreshConnections() }
                )
            }
            composable("discovery") {
                DiscoveryScreen(
                    connectedNodes = onlinePeers,
                    meshHealth = meshHealth,
                    cornerRadius = cornerRadiusSetting,
                    onNodeClick = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onShout = { discoveryViewModel.globalShout(it, encryptionEnabled, hopLimit, userProfile) }
                )
            }
            composable("chat/{peerId}/{peerName}", arguments = listOf(navArgument("peerId") { type = NavType.StringType }, navArgument("peerName") { type = NavType.StringType })) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"
                val ghostProfile = onlinePeers[peerId]

                ChatScreen(
                    peerId = peerId, peerName = peerName, messages = chatHistory,
                    isTyping = typingPeers.contains(peerId),
                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabled, selfDestructSeconds, hopLimit, userProfile) },
                    onSendImage = { uri: Uri -> chatViewModel.sendImage(uri, encryptionEnabled, selfDestructSeconds, hopLimit, userProfile) },
                    onSendVideo = { uri: Uri -> chatViewModel.sendVideo(uri, encryptionEnabled, selfDestructSeconds, hopLimit, userProfile) },
                    onStartVoice = { chatViewModel.startRecording() },
                    onStopVoice = { chatViewModel.stopRecording() },
                    onPlayVoice = { chatViewModel.playVoice(it) },
                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                    onTypingChange = { chatViewModel.sendTyping(it, userProfile) },
                    onBack = { navController.popBackStack() },
                    replyToMessage = replyToMessage,
                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                    onClearReply = { chatViewModel.clearReply() },
                    cornerRadius = cornerRadiusSetting,
                    transportType = ghostProfile?.transportType
                )
            }
            composable("settings") {
                SettingsScreen(
                    profile = userProfile, isDiscoveryEnabled = discoveryEnabled, isAdvertisingEnabled = advertisingEnabled,
                    isStealthMode = stealthMode, isHapticEnabled = hapticEnabled, isEncryptionEnabled = encryptionEnabled,
                    selfDestructSeconds = selfDestructSeconds, hopLimit = hopLimit,
                    packetsSent = packetsSent, packetsReceived = packetsReceived,
                    animationSpeed = animationSpeed, hapticIntensity = hapticIntensity,
                    messagePreview = messagePreview, autoReadReceipts = autoReadReceipts,
                    compactMode = compactMode, showTimestamps = showTimestamps,
                    connectionTimeout = connectionTimeout, scanInterval = scanInterval, maxImageSize = maxImageSize, themeMode = themeMode,
                    cornerRadius = cornerRadiusSetting, fontScale = fontScaleSetting,
                    isNearbyEnabled = isNearbyEnabled, isBluetoothEnabled = isBluetoothEnabled,
                    isLanEnabled = isLanEnabled, isWifiDirectEnabled = isWifiDirectEnabled,
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
                    packetCacheSize = 300,
                    onSetPacketCache = { },
                    onToggleNearby = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_NEARBY, it) },
                    onToggleBluetooth = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                    onToggleLan = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_LAN, it) },
                    onToggleWifiDirect = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                    onClearChat = { settingsViewModel.clearHistory() },
                    onNavigateToDocs = { navController.navigate("docs") },
                    onBack = { navController.popBackStack() },
                    onNavigateToTransfers = { navController.navigate("transfer_hub") }
                )
            }
            composable("transfer_hub") {
                TransferHubScreen(
                    transfers = activeTransfers,
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
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startMesh()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startMesh() {
        if (isServiceBound) return
        val prefs = getSharedPreferences(com.kai.ghostmesh.core.model.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val intent = Intent(this, MeshService::class.java).apply {
            putExtra("NICKNAME", prefs.getString("nick", "Peer"))
            putExtra("STEALTH", prefs.getBoolean("stealth", false))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            try { unbindService(serviceConnection) } catch (e: Exception) {}
            isServiceBound = false
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }
}
