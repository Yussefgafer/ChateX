package com.kai.ghostmesh.core.mesh.transports

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
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
        context.unregisterReceiver(receiver)
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
                synchronized(socket.outputStream) {
                    val dos = java.io.DataOutputStream(socket.outputStream)
                    dos.writeInt(data.size)
                    dos.write(data)
                    dos.flush()
                }
            }
        } else {
            connectedSockets.values.forEach { socket ->
                if (!socket.isClosed) {
                    synchronized(socket.outputStream) {
                        val dos = java.io.DataOutputStream(socket.outputStream)
                        dos.writeInt(data.size)
                        dos.write(data)
                        dos.flush()
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
            } catch (e: java.io.EOFException) {
                // Connection closed normally
            } catch (e: IOException) {
                // Connection lost
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
                    @Suppress("DEPRECATION")
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }
                    if (networkInfo?.isConnected == true) {
                        manager?.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed && !info.isGroupOwner) {
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
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers ->
                        peers.deviceList.forEach { device ->
                            val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
                            manager.connect(channel, config, null)
                        }
                    }
                }
            }
        }
    }
}
