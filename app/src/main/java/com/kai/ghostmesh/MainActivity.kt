package com.kai.ghostmesh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.ui.ChatScreen
import com.kai.ghostmesh.ui.Message

class MainActivity : ComponentActivity() {

    private lateinit var meshManager: MeshManager
    private val messages = mutableStateListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        meshManager = MeshManager(this) { sender, content ->
            messages.add(Message(sender, content, false))
        }

        requestPermissions()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ChatScreen(
                    messages = messages,
                    onSendMessage = { text ->
                        meshManager.sendMessage(text)
                        messages.add(Message("Me", text, true))
                    }
                )
            }
        }
    }

    private fun requestPermissions() {
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

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.all { it.value }) {
                meshManager.startMesh("Ghost_${Build.MODEL}")
                Toast.makeText(this, "Mesh Started!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied. App won't work.", Toast.LENGTH_LONG).show()
            }
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
