package com.kai.ghostmesh.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kai.ghostmesh.MainActivity
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

    // 📊 Live Diagnostics
    val totalPacketsSent = MutableStateFlow(0)
    val totalPacketsReceived = MutableStateFlow(0)

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
        
        startForeground(1, createNotification("ChateX Mesh is active"))
        
        if (meshManager == null) {
            meshManager = MeshManager(
                context = this,
                myNodeId = nodeId,
                myNickname = nickname,
                onPacketReceived = { packet ->
                    serviceScope.launch {
                        totalPacketsReceived.value++
                        _incomingPackets.emit(packet)
                        if (packet.type == PacketType.CHAT || packet.type == PacketType.IMAGE) {
                            showIncomingMessageNotification(packet)
                        }
                    }
                },
                onConnectionChanged = { ghosts ->
                    serviceScope.launch { _connectionUpdates.emit(ghosts) }
                },
                onProfileUpdate = { _, _, _ -> }
            )
            meshManager?.startMesh(nickname)
        }
        
        return START_STICKY
    }

    fun sendPacket(packet: Packet) {
        totalPacketsSent.value++
        meshManager?.sendPacket(packet)
    }

    fun updateMeshConfig(stealth: Boolean, nickname: String) {
        // In a real app, we'd restart discovery/advertising with new params
        // For MVP, if stealth changed, we restart
        meshManager?.stop()
        meshManager?.startMesh(nickname) // Start logic in Manager would check stealth ideally
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chatex_mesh", "ChateX Mesh Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle("ChateX Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showIncomingMessageNotification(packet: Packet) {
        val decrypted = SecurityManager.decrypt(packet.payload).take(50)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle(packet.senderName)
            .setContentText(if (packet.type == PacketType.IMAGE) "Sent a spectral image" else decrypted)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(packet.id.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager?.stop()
        serviceScope.cancel()
    }
}
