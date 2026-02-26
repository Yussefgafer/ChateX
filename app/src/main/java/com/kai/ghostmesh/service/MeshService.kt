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
import kotlinx.coroutines.flow.asSharedFlow

class MeshService : Service() {

    private val binder = MeshBinder()
    private var meshManager: MeshManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _incomingPackets = MutableSharedFlow<Packet>()
    val incomingPackets = _incomingPackets.asSharedFlow()

    private val _connectionUpdates = MutableSharedFlow<Map<String, String>>()
    val connectionUpdates = _connectionUpdates.asSharedFlow()

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
        
        startForeground(1, createNotification("ChateX is guarding the void..."))
        
        if (meshManager == null) {
            meshManager = MeshManager(
                context = this,
                myNodeId = nodeId,
                myNickname = nickname,
                onPacketReceived = { packet ->
                    serviceScope.launch {
                        _incomingPackets.emit(packet)
                        if (packet.type == PacketType.CHAT) {
                            showIncomingMessageNotification(packet)
                        }
                    }
                },
                onConnectionChanged = { ghosts ->
                    serviceScope.launch { _connectionUpdates.emit(ghosts) }
                },
                onProfileUpdate = { _, _, _ -> } // Handled via packets
            )
            meshManager?.startMesh(nickname)
        }
        
        return START_STICKY
    }

    fun sendPacket(packet: Packet) {
        meshManager?.sendPacket(packet)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chatex_mesh", "ChateX Mesh Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle("ChateX Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Placeholder
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showIncomingMessageNotification(packet: Packet) {
        val decrypted = SecurityManager.decrypt(packet.payload)
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "chatex_mesh")
            .setContentTitle("Message from ${packet.senderName}")
            .setContentText(decrypted)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(packet.id.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager?.stop()
        serviceScope.cancel()
    }
}
