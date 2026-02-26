package com.kai.ghostmesh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kai.ghostmesh.ui.*
import com.kai.ghostmesh.ui.theme.ChateXTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GhostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            ChateXTheme {
                val navController = rememberNavController()
                val chatHistory by viewModel.activeChatHistory.collectAsState()
                val onlineGhosts by viewModel.onlineGhosts.collectAsState()
                val knownProfiles by viewModel.allKnownProfiles.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                
                val discoveryEnabled by viewModel.isDiscoveryEnabled.collectAsState()
                val advertisingEnabled by viewModel.isAdvertisingEnabled.collectAsState()
                val hapticEnabled by viewModel.isHapticEnabled.collectAsState()
                val encryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
                val selfDestructSeconds by viewModel.selfDestructSeconds.collectAsState()
                val hopLimit by viewModel.hopLimit.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "radar") {
                        composable("radar") {
                            RadarScreen(connectedGhosts = onlineGhosts, onNavigateToChat = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") }, onNavigateToMessages = { navController.navigate("messages") }, onNavigateToSettings = { navController.navigate("settings") })
                        }
                        composable("messages") {
                            MessagesScreen(profiles = knownProfiles, onNavigateToChat = { id, name -> viewModel.setActiveChat(id); navController.navigate("chat/$id/$name") }, onNavigateToRadar = { navController.navigate("radar") }, onNavigateToSettings = { navController.navigate("settings") })
                        }
                        composable("chat/{ghostId}/{ghostName}", arguments = listOf(navArgument("ghostId") { type = NavType.StringType }, navArgument("ghostName") { type = NavType.StringType })) { backStackEntry ->
                            val ghostName = backStackEntry.arguments?.getString("ghostName") ?: "Unknown"
                            ChatScreen(ghostName = ghostName, messages = chatHistory, onSendMessage = { viewModel.sendMessage(it) }, onSendImage = { viewModel.sendImage(it) }, onBack = { viewModel.setActiveChat(null); navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(
                                profile = userProfile, isDiscoveryEnabled = discoveryEnabled, isAdvertisingEnabled = advertisingEnabled,
                                isHapticEnabled = hapticEnabled, isEncryptionEnabled = encryptionEnabled, selfDestructSeconds = selfDestructSeconds,
                                hopLimit = hopLimit, onProfileChange = { n, s -> viewModel.updateMyProfile(n, s) },
                                onToggleDiscovery = { viewModel.isDiscoveryEnabled.value = it }, onToggleAdvertising = { viewModel.isAdvertisingEnabled.value = it },
                                onToggleHaptic = { viewModel.isHapticEnabled.value = it }, onToggleEncryption = { viewModel.isEncryptionEnabled.value = it },
                                onSetSelfDestruct = { viewModel.selfDestructSeconds.value = it }, onSetHopLimit = { viewModel.hopLimit.value = it },
                                onClearChat = { viewModel.clearHistory() }, onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN); permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE); permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) viewModel.startMesh()
        }
        launcher.launch(permissions.toTypedArray())
    }
}
