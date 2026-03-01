package com.kai.ghostmesh.core.mesh.transports

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

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
        Thread {
            try {
                val serverSocket = ServerSocket(8888)
                while (true) {
                    val socket = serverSocket.accept()
                    handleIncomingSocket(socket)
                }
            } catch (e: IOException) {}
        }.start()
    }

    override fun stop() {
        context.unregisterReceiver(receiver)
        manager?.removeGroup(channel, null)
        connectedSockets.values.forEach { it.close() }
        connectedSockets.clear()
        nodeIdToName.clear()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray()
        if (endpointId != null) {
            connectedSockets[endpointId]?.outputStream?.write(data)
        } else {
            connectedSockets.values.forEach { it.outputStream.write(data) }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        val endpointId = socket.inetAddress.hostAddress ?: "unknown"
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = "WiFi Direct Peer"
        callback.onConnectionChanged(nodeIdToName.toMap())

        Thread {
            val buffer = ByteArray(1024 * 64)
            while (true) {
                try {
                    val bytes = socket.inputStream.read(buffer)
                    if (bytes > 0) {
                        callback.onPacketReceived(endpointId, String(buffer, 0, bytes))
                    }
                } catch (e: IOException) {
                    connectedSockets.remove(endpointId)
                    nodeIdToName.remove(endpointId)
                    callback.onConnectionChanged(nodeIdToName.toMap())
                    break
                }
            }
        }.start()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager?.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed && !info.isGroupOwner) {
                                Thread {
                                    try {
                                        val socket = Socket()
                                        socket.connect(InetSocketAddress(info.groupOwnerAddress, 8888), 5000)
                                        handleIncomingSocket(socket)
                                    } catch (e: IOException) {}
                                }.start()
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
