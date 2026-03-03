package com.kai.ghostmesh.core.mesh.transports

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import android.net.wifi.p2p.*
import android.os.Build
import com.kai.ghostmesh.core.util.GhostLog as Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class WifiDirectTransport(
    override val name: String = "WiFiDirect",
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)
    private val gson = Gson()
    private val connectedSockets = ConcurrentHashMap<String, Socket>()
    private val nodeIdToName = ConcurrentHashMap<String, String>()
    private val socketExecutor = java.util.concurrent.Executors.newCachedThreadPool()

    private val lastConnectionAttempt = ConcurrentHashMap<String, Long>()
    private val CONNECTION_COOLDOWN = TimeUnit.MINUTES.toMillis(2)

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun start(nickname: String, isStealth: Boolean) {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, intentFilter)
        manager?.discoverPeers(channel, null)
        startServer()
    }

    private fun startServer() {
        socketExecutor.execute {
            try {
                val serverSocket = ServerSocket(8888)
                while (!serverSocket.isClosed) {
                    val socket = serverSocket.accept()
                    handleIncomingSocket(socket)
                }
            } catch (e: IOException) {}
        }
    }

    override fun stop() {
        try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        manager?.removeGroup(channel, null)
        connectedSockets.values.forEach { try { it.close() } catch (e: Exception) {} }
        connectedSockets.clear()
        nodeIdToName.clear()
        socketExecutor.shutdownNow()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray(Charsets.UTF_8)

        if (endpointId != null) {
            val socket = connectedSockets[endpointId]
            if (socket != null && !socket.isClosed) {
                try {
                    synchronized(socket.outputStream) {
                        val dos = java.io.DataOutputStream(socket.outputStream)
                        dos.writeInt(data.size)
                        dos.write(data)
                        dos.flush()
                    }
                } catch (e: Exception) {
                    connectedSockets.remove(endpointId)
                }
            }
        } else {
            connectedSockets.forEach { (id, socket) ->
                if (!socket.isClosed) {
                    try {
                        synchronized(socket.outputStream) {
                            val dos = java.io.DataOutputStream(socket.outputStream)
                            dos.writeInt(data.size)
                            dos.write(data)
                            dos.flush()
                        }
                    } catch (e: Exception) {
                        connectedSockets.remove(id)
                    }
                }
            }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        val endpointId = socket.inetAddress.hostAddress ?: "unknown"
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = "WiFi Direct Peer"
        callback.onConnectionChanged(nodeIdToName.toMap())

        socketExecutor.execute {
            try {
                val dis = java.io.DataInputStream(socket.getInputStream())
                while (!socket.isClosed) {
                    val length = dis.readInt()
                    if (length > 1024 * 1024) throw IOException("Packet too large: $length")
                    val payload = ByteArray(length)
                    dis.readFully(payload)
                    val json = String(payload, Charsets.UTF_8)
                    callback.onPacketReceived(endpointId, json)
                }
            } catch (e: Exception) {
                // Connection lost or closed
            } finally {
                connectedSockets.remove(endpointId)
                nodeIdToName.remove(endpointId)
                callback.onConnectionChanged(nodeIdToName.toMap())
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestGroupInfo(channel) { group ->
                        if (group != null && !group.isGroupOwner) {
                            manager.requestConnectionInfo(channel) { info ->
                                if (info.groupFormed) {
                                    socketExecutor.execute {
                                        try {
                                            val socket = Socket()
                                            socket.connect(InetSocketAddress(info.groupOwnerAddress, 8888), 5000)
                                            handleIncomingSocket(socket)
                                        } catch (e: IOException) {}
                                    }
                                }
                            }
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        peers.deviceList.filter { it.status == WifiP2pDevice.AVAILABLE }.forEach { device ->
                            val now = System.currentTimeMillis()
                            val lastAttempt = lastConnectionAttempt[device.deviceAddress] ?: 0L

                            // Prevent aggressive connection loops: Cooldown + simple deterministic selection
                            if (now - lastAttempt > CONNECTION_COOLDOWN) {
                                // Deterministic: device with lexicographically smaller address initiates
                                if (myNodeId.takeLast(12) < device.deviceAddress.replace(":", "").takeLast(12)) {
                                    val config = WifiP2pConfig().apply {
                                        deviceAddress = device.deviceAddress
                                        groupOwnerIntent = 0 // Low intent to be GO
                                    }
                                    lastConnectionAttempt[device.deviceAddress] = now
                                    manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                                        override fun onSuccess() { Log.d(name, "Connect request accepted for ${device.deviceName}") }
                                        override fun onFailure(reason: Int) { Log.e(name, "Connect failed: $reason") }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
