package com.kai.ghostmesh

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kai.ghostmesh.features.chat.*
import com.kai.ghostmesh.features.discovery.*
import com.kai.ghostmesh.features.settings.*
import com.kai.ghostmesh.features.messages.*
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.ui.theme.ChateXTheme
import com.kai.ghostmesh.service.MeshService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestIgnoreBatteryOptimizations()
        checkAndRequestPermissions()

        setContent {
            val messagesViewModel: MessagesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val discoveryViewModel: DiscoveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val themeMode by settingsViewModel.themeMode.collectAsState()

            ChateXTheme(darkTheme = when(themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainContent(
                            messagesViewModel, chatViewModel, discoveryViewModel, settingsViewModel,
                            navController
                        )
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
        navController: androidx.navigation.NavHostController
    ) {
        val recentChats by messagesViewModel.recentChats.collectAsState()
        
        val chatHistory by chatViewModel.messages.collectAsState()
        val typingGhosts by chatViewModel.typingGhosts.collectAsState()
        val replyToMessage by chatViewModel.replyToMessage.collectAsState()

        val onlineGhosts by discoveryViewModel.connectedNodes.collectAsState()
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
        val fontScale by settingsViewModel.fontScale.collectAsState()
        val packetsSent by settingsViewModel.packetsSent.collectAsState()
        val packetsReceived by settingsViewModel.packetsReceived.collectAsState()
        val packetCacheSize by settingsViewModel.packetCacheSize.collectAsState()
        val isNearbyEnabled by settingsViewModel.isNearbyEnabled.collectAsState()
        val isBluetoothEnabled by settingsViewModel.isBluetoothEnabled.collectAsState()
        val isLanEnabled by settingsViewModel.isLanEnabled.collectAsState()
        val isWifiDirectEnabled by settingsViewModel.isWifiDirectEnabled.collectAsState()

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
                    recentChats = recentChats,
                    cornerRadius = cornerRadiusSetting,
                    onNavigateToChat = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onNavigateToRadar = { navController.navigate("discovery") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onRefresh = { messagesViewModel.refreshConnections() }
                )
            }
            composable("discovery") {
                DiscoveryScreen(
                    connectedNodes = onlineGhosts,
                    meshHealth = meshHealth,
                    cornerRadius = cornerRadiusSetting,
                    onNodeClick = { id, name -> chatViewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onShout = { discoveryViewModel.globalShout(it, encryptionEnabled, hopLimit, userProfile) }
                )
            }
            composable("chat/{ghostId}/{ghostName}", arguments = listOf(navArgument("ghostId") { type = NavType.StringType }, navArgument("ghostName") { type = NavType.StringType })) { backStackEntry ->
                val ghostId = backStackEntry.arguments?.getString("ghostId") ?: ""
                val ghostName = backStackEntry.arguments?.getString("ghostName") ?: "Unknown"
                ChatScreen(
                    ghostId = ghostId, ghostName = ghostName, messages = chatHistory,
                    isTyping = typingGhosts.contains(ghostId),
                    onSendMessage = { chatViewModel.sendMessage(it, encryptionEnabled, selfDestructSeconds, hopLimit, userProfile) },
                    onSendImage = { uri: Uri -> chatViewModel.sendImage(uri, encryptionEnabled, selfDestructSeconds, hopLimit, userProfile) },
                    onStartVoice = { chatViewModel.startRecording() },
                    onStopVoice = { chatViewModel.stopRecording() },
                    onPlayVoice = { chatViewModel.playVoice(it) },
                    onDeleteMessage = { chatViewModel.deleteMessage(it) },
                    onTypingChange = { chatViewModel.sendTyping(it, userProfile) },
                    onBack = { chatViewModel.setActiveChat(null); chatViewModel.clearReply(); navController.popBackStack() },
                    replyToMessage = replyToMessage,
                    onSetReply = { id, content, sender -> chatViewModel.setReplyTo(id, content, sender) },
                    onClearReply = { chatViewModel.clearReply() },
                    cornerRadius = cornerRadiusSetting
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
                    cornerRadius = cornerRadiusSetting, fontScale = fontScale,
                    isNearbyEnabled = isNearbyEnabled, isBluetoothEnabled = isBluetoothEnabled,
                    isLanEnabled = isLanEnabled, isWifiDirectEnabled = isWifiDirectEnabled,
                    onProfileChange = { n, s, c -> settingsViewModel.updateMyProfile(n, s, c) },
                    onToggleDiscovery = { settingsViewModel.updateSetting("discovery", it) },
                    onToggleAdvertising = { settingsViewModel.updateSetting("advertising", it) },
                    onToggleStealth = { settingsViewModel.updateSetting("stealth", it) },
                    onToggleHaptic = { settingsViewModel.updateSetting("haptic", it) },
                    onToggleEncryption = { settingsViewModel.updateSetting("encryption", it) },
                    onSetSelfDestruct = { settingsViewModel.updateSetting("burn", it) },
                    onSetHopLimit = { settingsViewModel.updateSetting("hops", it) },
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
                    packetCacheSize = packetCacheSize,
                    onSetPacketCache = { settingsViewModel.updateSetting("net_packet_cache", it) },
                    onToggleNearby = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_NEARBY, it) },
                    onToggleBluetooth = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                    onToggleLan = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_LAN, it) },
                    onToggleWifiDirect = { settingsViewModel.updateSetting(AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                    onClearChat = { settingsViewModel.clearHistory() }, onBack = { navController.popBackStack() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN); permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE); permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES); permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) startMesh()
        }
        launcher.launch(permissions.toTypedArray())
    }

    private fun startMesh() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val nickname = prefs.getString("nick", "Ghost")!!
        val isStealth = prefs.getBoolean("stealth", false)
        val intent = Intent(this, MeshService::class.java).apply {
            putExtra("NICKNAME", nickname)
            putExtra("STEALTH", isStealth)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    @androidx.annotation.OptIn(androidx.core.os.BuildCompat.PrereleaseSdkCheck::class)
    @android.annotation.SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try { startActivity(intent) } catch (e: Exception) { /* User denied */ }
            }
        }
    }
}
