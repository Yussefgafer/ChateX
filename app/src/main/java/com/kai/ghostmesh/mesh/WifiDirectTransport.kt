package com.kai.ghostmesh.mesh

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class WifiDirectTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, Looper.getMainLooper(), null)
    private val gson = Gson()
    private val executor = Executors.newCachedThreadPool()

    private var serverSocket: ServerSocket? = null
    private val PORT = 8888
    private val connectedSockets = mutableMapOf<String, Socket>()
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        peers.deviceList.forEach { device ->
                            if (device.status == WifiP2pDevice.AVAILABLE) {
                                connectToDevice(device)
                            }
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager?.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed) {
                                if (info.isGroupOwner) {
                                    // Already running server
                                } else {
                                    executor.execute { connectToServer(info.groupOwnerAddress) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun start(nickname: String, isStealth: Boolean) {
        if (manager == null || channel == null) return

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, intentFilter)
        isReceiverRegistered = true

        startServer()
        if (!isStealth) {
            discoverPeers()
        }
    }

    private fun startServer() {
        executor.execute {
            try {
                serverSocket = ServerSocket(PORT)
                while (!Thread.currentThread().isInterrupted && !serverSocket!!.isClosed) {
                    val socket = serverSocket?.accept() ?: break
                    handleSocket(socket)
                }
            } catch (e: Exception) {
                Log.e("WifiDirect", "Server error", e)
            }
        }
    }

    private fun handleSocket(socket: Socket) {
        executor.execute {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = socket.getOutputStream()

                // Handshake: format "GHOST_WD|v1|nodeId"
                out.write(("GHOST_WD|v1|$myNodeId\n").toByteArray())
                val handshakeLine = reader.readLine() ?: return@execute
                val parts = handshakeLine.split("|")
                if (parts.getOrNull(0) != "GHOST_WD") {
                    socket.close()
                    return@execute
                }
                val remoteNodeId = parts.getOrNull(2) ?: return@execute

                synchronized(connectedSockets) {
                    connectedSockets[remoteNodeId] = socket
                }

                callback.onConnectionChanged(synchronized(connectedSockets) { connectedSockets.keys.associateWith { it } })

                while (!Thread.currentThread().isInterrupted && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    callback.onPacketReceived(remoteNodeId, line)
                }
            } catch (e: Exception) {
                Log.e("WifiDirect", "Socket error", e)
            } finally {
                cleanupSocket(socket)
            }
        }
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })
    }

    private fun connectToServer(address: InetAddress) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(address, PORT), 5000)
            handleSocket(socket)
        } catch (e: Exception) {
            Log.e("WifiDirect", "Connect to server failed", e)
        }
    }

    private fun cleanupSocket(socket: Socket) {
        synchronized(connectedSockets) {
            val nodeId = connectedSockets.filterValues { it == socket }.keys.firstOrNull()
            if (nodeId != null) {
                connectedSockets.remove(nodeId)
                callback.onConnectionChanged(connectedSockets.keys.associateWith { it })
            }
        }
        try { socket.close() } catch (e: Exception) {}
    }

    private fun discoverPeers() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirect", "Discovery started")
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirect", "Discovery failed: $reason")
            }
        })
    }

    override fun stop() {
        if (isReceiverRegistered) {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
        try { serverSocket?.close() } catch (e: Exception) {}
        synchronized(connectedSockets) {
            connectedSockets.values.forEach { try { it.close() } catch (e: Exception) {} }
            connectedSockets.clear()
        }
        manager?.stopPeerDiscovery(channel, null)
        manager?.removeGroup(channel, null)
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        executor.execute {
            val json = gson.toJson(packet) + "\n"
            if (endpointId != null) {
                connectedSockets[endpointId]?.getOutputStream()?.write(json.toByteArray())
            } else {
                connectedSockets.values.forEach {
                    try { it.getOutputStream().write(json.toByteArray()) } catch (e: Exception) {}
                }
            }
        }
    }
}
