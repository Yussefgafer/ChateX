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
                val chatHistory by viewModel.chatHistory.collectAsState()
                val connectedGhosts by viewModel.connectedGhosts.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                
                val discoveryEnabled by viewModel.isDiscoveryEnabled.collectAsState()
                val advertisingEnabled by viewModel.isAdvertisingEnabled.collectAsState()
                val hapticEnabled by viewModel.isHapticEnabled.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "radar") {
                        composable("radar") {
                            RadarScreen(
                                connectedGhosts = connectedGhosts,
                                onNavigateToChat = { id, name -> 
                                    viewModel.setActiveChat(id)
                                    navController.navigate("chat/$id/$name") 
                                },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable(
                            route = "chat/{ghostId}/{ghostName}",
                            arguments = listOf(
                                navArgument("ghostId") { type = NavType.StringType },
                                navArgument("ghostName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val ghostId = backStackEntry.arguments?.getString("ghostId") ?: ""
                            val ghostName = backStackEntry.arguments?.getString("ghostName") ?: "Unknown"
                            
                            ChatScreen(
                                ghostName = ghostName,
                                messages = chatHistory[ghostId] ?: emptyList(),
                                onSendMessage = { viewModel.sendMessage(it) },
                                onBack = { 
                                    viewModel.setActiveChat(null)
                                    navController.popBackStack() 
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                profile = userProfile,
                                isDiscoveryEnabled = discoveryEnabled,
                                isAdvertisingEnabled = advertisingEnabled,
                                isHapticEnabled = hapticEnabled,
                                onProfileChange = { name, status -> viewModel.updateMyProfile(name, status) },
                                onToggleDiscovery = { viewModel.isDiscoveryEnabled.value = it },
                                onToggleAdvertising = { viewModel.isAdvertisingEnabled.value = it },
                                onToggleHaptic = { viewModel.isHapticEnabled.value = it },
                                onClearChat = { viewModel.clearHistory() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startMesh()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            startMesh()
        } else {
            Toast.makeText(this, "ChateX needs permissions ⚡", Toast.LENGTH_LONG).show()
        }
    }

    private fun startMesh() {
        viewModel.startMesh()
    }
}
