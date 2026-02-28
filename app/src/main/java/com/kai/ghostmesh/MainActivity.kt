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
import com.kai.ghostmesh.data.local.AppDatabase
import com.kai.ghostmesh.data.repository.GhostRepository
import com.kai.ghostmesh.service.MeshService
import com.kai.ghostmesh.ui.*
import com.kai.ghostmesh.ui.theme.ChateXTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GhostViewModel
    private var meshService: MeshService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            viewModel.bindService(meshService!!)
        }
        override fun onServiceDisconnected(name: ComponentName?) { meshService = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val repository = GhostRepository(db.messageDao(), db.profileDao())
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GhostViewModel(application, repository) as T
            }
        })[GhostViewModel::class.java]

        Intent(this, MeshService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }

        requestIgnoreBatteryOptimizations()
        checkAndRequestPermissions()

        setContent {
            val cornerRadiusSetting by viewModel.cornerRadius.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            ChateXTheme(darkTheme = when(themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val errorMsg by viewModel.errorMessage.collectAsState()

                LaunchedEffect(errorMsg) {
                    errorMsg?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearErrorMessage()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainContent(viewModel, navController)
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(viewModel: GhostViewModel, navController: androidx.navigation.NavHostController) {
        val chatHistory by viewModel.messages.collectAsState()
        val onlineGhosts by viewModel.connectedNodes.collectAsState()
        val typingGhosts by viewModel.typingGhosts.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        
        val discoveryEnabled by viewModel.isDiscoveryEnabled.collectAsState()
        val advertisingEnabled by viewModel.isAdvertisingEnabled.collectAsState()
        val stealthMode by viewModel.isStealthMode.collectAsState()
        val hapticEnabled by viewModel.isHapticEnabled.collectAsState()
        val encryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
        val selfDestructSeconds by viewModel.selfDestructSeconds.collectAsState()
        val hopLimit by viewModel.hopLimit.collectAsState()
        
        val animationSpeed by viewModel.animationSpeed.collectAsState()
        val hapticIntensity by viewModel.hapticIntensity.collectAsState()
        val messagePreview by viewModel.messagePreview.collectAsState()
        val autoReadReceipts by viewModel.autoReadReceipts.collectAsState()
        val compactMode by viewModel.compactMode.collectAsState()
        val showTimestamps by viewModel.showTimestamps.collectAsState()
        val connectionTimeout by viewModel.connectionTimeout.collectAsState()
        val scanInterval by viewModel.scanInterval.collectAsState()
        val maxImageSize by viewModel.maxImageSize.collectAsState()
        val themeMode by viewModel.themeMode.collectAsState()
        val cornerRadiusSetting by viewModel.cornerRadius.collectAsState()
        val fontScale by viewModel.fontScale.collectAsState()

        val packetsSent by viewModel.packetsSent.collectAsState()
        val packetsReceived by viewModel.packetsReceived.collectAsState()
        val packetCacheSize by viewModel.packetCacheSize.collectAsState()

        val isNearbyEnabled by viewModel.isNearbyEnabled.collectAsState()
        val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
        val isLanEnabled by viewModel.isLanEnabled.collectAsState()
        val isWifiDirectEnabled by viewModel.isWifiDirectEnabled.collectAsState()
        
        val meshHealth by viewModel.meshHealth.collectAsState()
        val replyToMessage by viewModel.replyToMessage.collectAsState()

        NavHost(
            navController = navController,
            startDestination = "messages",
            enterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            exitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popEnterTransition = { fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) },
            popExitTransition = { fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
        ) {
            composable("messages") {
                val recentChats by viewModel.recentChats.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                MessagesScreen(
                    recentChats = recentChats,
                    isRefreshing = isRefreshing,
                    cornerRadius = cornerRadiusSetting,
                    onNavigateToChat = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onNavigateToRadar = { navController.navigate("discovery") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onRefresh = { viewModel.refreshConnections() }
                )
            }
            composable("discovery") {
                DiscoveryScreen(
                    connectedNodes = onlineGhosts,
                    meshHealth = meshHealth,
                    cornerRadius = cornerRadiusSetting,
                    onNodeClick = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") },
                    onShout = { viewModel.globalShout(it) }
                )
            }
            composable("chat/{ghostId}/{ghostName}", arguments = listOf(navArgument("ghostId") { type = NavType.StringType }, navArgument("ghostName") { type = NavType.StringType })) { backStackEntry ->
                val ghostId = backStackEntry.arguments?.getString("ghostId") ?: ""
                val ghostName = backStackEntry.arguments?.getString("ghostName") ?: "Unknown"
                ChatScreen(
                    ghostId = ghostId, ghostName = ghostName, messages = chatHistory,
                    isTyping = typingGhosts.contains(ghostId),
                    onSendMessage = { viewModel.sendMessage(it) },
                    onSendImage = { uri: Uri -> viewModel.sendImage(uri) },
                    onStartVoice = { viewModel.startRecording() },
                    onStopVoice = { viewModel.stopRecording() },
                    onPlayVoice = { viewModel.playVoice(it) },
                    onDeleteMessage = { viewModel.deleteMessage(it) },
                    onTypingChange = { viewModel.sendTyping(it) },
                    onBack = { viewModel.setActiveChat(null); viewModel.clearReply(); navController.popBackStack() },
                    replyToMessage = replyToMessage,
                    onSetReply = { id, content, sender -> viewModel.setReplyTo(id, content, sender) },
                    onClearReply = { viewModel.clearReply() },
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
                    onProfileChange = { n, s, c -> viewModel.updateMyProfile(n, s, c) },
                    onToggleDiscovery = { viewModel.isDiscoveryEnabled.value = it; viewModel.updateSetting("discovery", it) },
                    onToggleAdvertising = { viewModel.isAdvertisingEnabled.value = it; viewModel.updateSetting("advertising", it) },
                    onToggleStealth = { viewModel.isStealthMode.value = it; viewModel.updateSetting("stealth", it) },
                    onToggleHaptic = { viewModel.isHapticEnabled.value = it; viewModel.updateSetting("haptic", it) },
                    onToggleEncryption = { viewModel.isEncryptionEnabled.value = it; viewModel.updateSetting("encryption", it) },
                    onSetSelfDestruct = { viewModel.selfDestructSeconds.value = it; viewModel.updateSetting("burn", it) },
                    onSetHopLimit = { viewModel.hopLimit.value = it; viewModel.updateSetting("hops", it) },
                    onSetAnimationSpeed = { viewModel.animationSpeed.value = it; viewModel.updateSetting("animation_speed", it) },
                    onSetHapticIntensity = { viewModel.hapticIntensity.value = it; viewModel.updateSetting("haptic_intensity", it) },
                    onToggleMessagePreview = { viewModel.messagePreview.value = it; viewModel.updateSetting("message_preview", it) },
                    onToggleAutoReadReceipts = { viewModel.autoReadReceipts.value = it; viewModel.updateSetting("auto_read_receipts", it) },
                    onToggleCompactMode = { viewModel.compactMode.value = it; viewModel.updateSetting("compact_mode", it) },
                    onToggleShowTimestamps = { viewModel.showTimestamps.value = it; viewModel.updateSetting("show_timestamps", it) },
                    onSetConnectionTimeout = { viewModel.connectionTimeout.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_CONN_TIMEOUT, it) },
                    onSetScanInterval = { viewModel.scanInterval.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_SCAN_INTERVAL, it) },
                    onSetMaxImageSize = { viewModel.maxImageSize.value = it; viewModel.updateSetting("max_image_size", it) },
                    onSetThemeMode = { viewModel.themeMode.value = it; viewModel.updateSetting("theme_mode", it) },
                    onSetCornerRadius = { viewModel.cornerRadius.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_CORNER_RADIUS, it) },
                    onSetFontScale = { viewModel.fontScale.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_FONT_SCALE, it) },
                    packetCacheSize = packetCacheSize,
                    onSetPacketCache = { viewModel.packetCacheSize.value = it; viewModel.updateSetting("net_packet_cache", it) },
                    onToggleNearby = { viewModel.isNearbyEnabled.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_ENABLE_NEARBY, it) },
                    onToggleBluetooth = { viewModel.isBluetoothEnabled.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_ENABLE_BLUETOOTH, it) },
                    onToggleLan = { viewModel.isLanEnabled.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_ENABLE_LAN, it) },
                    onToggleWifiDirect = { viewModel.isWifiDirectEnabled.value = it; viewModel.updateSetting(com.kai.ghostmesh.model.AppConfig.KEY_ENABLE_WIFI_DIRECT, it) },
                    onClearChat = { viewModel.clearHistory() }, onBack = { navController.popBackStack() }
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
            if (results.all { it.value }) viewModel.startMesh()
        }
        launcher.launch(permissions.toTypedArray())
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
