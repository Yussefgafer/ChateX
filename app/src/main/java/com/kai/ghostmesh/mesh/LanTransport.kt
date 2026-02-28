package com.kai.ghostmesh.mesh

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class LanTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_ghostmesh._tcp"
    private var serviceName = "Ghost_$myNodeId"
    private var serverSocket: ServerSocket? = null
    private var localPort: Int = -1
    private val gson = Gson()
    private val executor = Executors.newCachedThreadPool()
    private val connectedSockets = mutableMapOf<String, Socket>()
    private val endpointToNodeId = mutableMapOf<String, String>()

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            serviceName = serviceInfo.serviceName
        }
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            callback.onError("LAN Registration failed: $errorCode")
        }
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceType == serviceType && serviceInfo.serviceName != serviceName) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        executor.execute { connectToService(serviceInfo) }
                    }
                })
            }
        }
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        override fun onDiscoveryStopped(regType: String) {}
        override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
        override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
    }

    override fun start(nickname: String, isStealth: Boolean) {
        executor.execute {
            try {
                serverSocket = ServerSocket(0).also {
                    localPort = it.localPort
                    startServerAcceptance(it)
                }
                if (!isStealth) {
                    registerService(localPort)
                    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                }
            } catch (e: Exception) {
                callback.onError("LAN Start Error: ${e.message}")
            }
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Ghost_$myNodeId"
            serviceType = this@LanTransport.serviceType
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun startServerAcceptance(serverSocket: ServerSocket) {
        executor.execute {
            while (!Thread.currentThread().isInterrupted && !serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    handleIncomingConnection(socket)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun handleIncomingConnection(socket: Socket) {
        executor.execute {
            try {
                val input = socket.getInputStream().bufferedReader()
                val handshakeLine = input.readLine() ?: return@execute
                // Format: "GHOST_HANDSHAKE|v1|nodeId"
                val parts = handshakeLine.split("|")
                if (parts.getOrNull(0) != "GHOST_HANDSHAKE") {
                    socket.close()
                    return@execute
                }
                val remoteNodeId = parts.getOrNull(2) ?: return@execute

                synchronized(connectedSockets) {
                    connectedSockets[remoteNodeId] = socket
                    endpointToNodeId[socket.remoteSocketAddress.toString()] = remoteNodeId
                    callback.onConnectionChanged(endpointToNodeId.values.associateWith { it })
                }

                while (!Thread.currentThread().isInterrupted && !socket.isClosed) {
                    val json = input.readLine() ?: break
                    callback.onPacketReceived(remoteNodeId, json)
                }
            } catch (e: Exception) {
                // Connection closed
            } finally {
                cleanupSocket(socket)
            }
        }
    }

    private fun connectToService(serviceInfo: NsdServiceInfo) {
        try {
            val socket = Socket(serviceInfo.host, serviceInfo.port)
            socket.getOutputStream().write(("GHOST_HANDSHAKE|v1|$myNodeId\n").toByteArray())
            handleIncomingConnection(socket)
        } catch (e: Exception) {
            Log.e("LanTransport", "Connect failed: ${e.message}")
        }
    }

    private fun cleanupSocket(socket: Socket) {
        synchronized(connectedSockets) {
            val nodeId = endpointToNodeId.remove(socket.remoteSocketAddress.toString())
            if (nodeId != null) {
                connectedSockets.remove(nodeId)
                callback.onConnectionChanged(endpointToNodeId.values.associateWith { it })
            }
        }
        try { socket.close() } catch (e: Exception) {}
    }

    override fun stop() {
        try { nsdManager.unregisterService(registrationListener) } catch (e: Exception) {}
        try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
        synchronized(connectedSockets) {
            connectedSockets.values.forEach { try { it.close() } catch (e: Exception) {} }
            connectedSockets.clear()
            endpointToNodeId.clear()
        }
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
