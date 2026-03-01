package com.kai.ghostmesh.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kai.ghostmesh.MainActivity
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.AppConfig
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.security.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class MeshService : Service() {

    private val binder = MeshBinder()
    private lateinit var meshManager: MeshManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentPeerCount = 0
    private var currentBatteryLevel: Int = 100

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                if (batteryPct != currentBatteryLevel) {
                    currentBatteryLevel = batteryPct
                    onBatteryChanged(batteryPct)
                }
            }
        }
    }

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        meshManager = (application as GhostApplication).container.meshManager
        createNotificationChannel()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        serviceScope.launch {
            meshManager.incomingPackets.collect { packet ->
                if (packet.type == PacketType.CHAT || packet.type == PacketType.IMAGE || packet.type == PacketType.VOICE) {
                    showIncomingMessageNotification(packet)
                }
            }
        }

        serviceScope.launch {
            meshManager.connectionUpdates.collect { ghosts ->
                currentPeerCount = ghosts.size
                updateForegroundNotification(currentPeerCount)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nickname = intent?.getStringExtra("NICKNAME") ?: "User"
        val isStealth = intent?.getBooleanExtra("STEALTH", false) ?: false
        
        updateForegroundNotification(0)
        meshManager.startMesh(nickname, isStealth)
        meshManager.updateBattery(currentBatteryLevel)
        
        return START_STICKY
    }

    private fun onBatteryChanged(batteryPct: Int) {
        meshManager.updateBattery(batteryPct)
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
        val decrypted = try { SecurityManager.decrypt(packet.payload, if(packet.receiverId == "ALL") null else packet.senderId) } catch (e: Exception) { "Encrypted message" }
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        meshManager.stop()
        serviceScope.cancel()
    }
}
