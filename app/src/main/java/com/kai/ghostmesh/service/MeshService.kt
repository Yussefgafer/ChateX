package com.kai.ghostmesh.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kai.ghostmesh.MainActivity
import com.kai.ghostmesh.mesh.FileTransferManager
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import com.kai.ghostmesh.security.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class MeshService : Service() {

    private val binder = MeshBinder()
    private var meshManager: MeshManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _incomingPackets = MutableSharedFlow<Packet>()
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _connectionUpdates = MutableSharedFlow<Map<String, String>>()
    val connectionUpdates = _connectionUpdates.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asSharedFlow()

    val totalPacketsSent = MutableStateFlow(0)
    val totalPacketsReceived = MutableStateFlow(0)
    private var currentPeerCount = 0
    
    private var fileTransferManager: FileTransferManager? = null

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nickname = intent?.getStringExtra("NICKNAME") ?: "User"
        val nodeId = intent?.getStringExtra("NODE_ID") ?: "Unknown"
        val isStealth = intent?.getBooleanExtra("STEALTH", false) ?: false
        
        updateForegroundNotification(0)
        
        if (meshManager == null) {
            meshManager = MeshManager(
                context = this,
                myNodeId = nodeId,
                myNickname = nickname,
                onPacketReceived = { packet ->
                    serviceScope.launch {
                        totalPacketsReceived.value++
                        _incomingPackets.emit(packet)
                        if (packet.type == PacketType.CHAT || packet.type == PacketType.IMAGE || packet.type == PacketType.VOICE) {
                            showIncomingMessageNotification(packet)
                        }
                    }
                },
                onConnectionChanged = { ghosts ->
                    serviceScope.launch {
                        currentPeerCount = ghosts.size
                        updateForegroundNotification(currentPeerCount)
                        _connectionUpdates.emit(ghosts)
                    }
                },
                onProfileUpdate = { _, _, _ -> },
                onTransportError = { error ->
                    Log.e("MeshService", "Transport Error: $error")
                }
            )
            meshManager?.startMesh(nickname, isStealth)
        }
        
        return START_STICKY
    }

    fun sendPacket(packet: Packet) {
        totalPacketsSent.value++
        meshManager?.sendPacket(packet)
    }

    fun updateMeshConfig(stealth: Boolean, nickname: String) {
        meshManager?.startMesh(nickname, stealth)
        updateForegroundNotification(currentPeerCount)
    }

    private fun updateForegroundNotification(peerCount: Int) {
        val text = if (peerCount == 0) "Scanning the void..." else "Linked to $peerCount spectral nodes"
        startForeground(1, createNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("chatex_mesh", "ChateX Mesh Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle("ChateX Mesh Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showIncomingMessageNotification(packet: Packet) {
        val decrypted = try { SecurityManager.decrypt(packet.payload) } catch (e: Exception) { "Encrypted message" }
        val previewText = when(packet.type) {
            PacketType.IMAGE -> "Photo"
            PacketType.VOICE -> "Voice message"
            PacketType.FILE -> "File"
            PacketType.CHAT -> decrypted.take(100)
            else -> "New message"
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ghostId", packet.senderId)
            putExtra("ghostName", packet.senderName)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle(packet.senderName)
            .setContentText(previewText)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(previewText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(packet.senderId.hashCode(), notification)
    }
    
    fun sendFile(uri: Uri, recipientId: String, fileName: String, onProgress: (Float) -> Unit) {
        if (fileTransferManager == null) {
            fileTransferManager = FileTransferManager(
                context = this,
                connectionsClient = com.google.android.gms.nearby.Nearby.getConnectionsClient(this),
                myNodeId = "",
                onFileProgress = { name, _, progress -> 
                    if (name == fileName) onProgress(progress)
                },
                onFileComplete = { name, _, path -> 
                    showFileTransferNotification(name, "Completed", path)
                },
                onFileError = { name, _, error -> 
                    showFileTransferNotification(name, "Failed: $error", null)
                }
            )
        }
    }
    
    private fun showFileTransferNotification(fileName: String, status: String, filePath: String?) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle("File Transfer")
            .setContentText("$fileName: $status")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(fileName.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager?.stop()
        serviceScope.cancel()
    }
}
