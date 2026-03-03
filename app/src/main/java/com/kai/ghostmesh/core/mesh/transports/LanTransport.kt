package com.kai.ghostmesh.core.mesh.transports

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.kai.ghostmesh.R
import com.kai.ghostmesh.core.util.GhostLog as Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

class LanTransport(
    override val name: String = "LAN",
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_ghostmesh._tcp."
    private val gson = Gson()
    private var serverSocket: ServerSocket? = null
    private val connectedSockets = ConcurrentHashMap<String, Socket>()
    private val nodeIdToName = ConcurrentHashMap<String, String>()

    private val transportScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentDiscoveryInterval = 10000L

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun setScanInterval(intervalMs: Long) {
        this.currentDiscoveryInterval = intervalMs
    }

    override fun start(nickname: String, isStealth: Boolean) {
        try {
            serverSocket = ServerSocket(0)
            startServerLoop()

            if (!isStealth) {
                registerService(nickname)
            }
            discoverServices()
        } catch (e: IOException) {
            Log.e("LanTransport", "Failed to start LAN transport", e)
            callback.onError("LAN start failed: ${e.message}")
        }
    }

    private fun startServerLoop() {
        transportScope.launch {
            try {
                while (isActive && serverSocket?.isClosed == false) {
                    val socket = try { serverSocket?.accept() } catch (e: Exception) { null }
                    socket?.let { handleIncomingSocket(it) }
                }
            } catch (e: Exception) {
                Log.e("LanTransport", "Server loop error", e)
                if (serverSocket?.isClosed == false) {
                    callback.onError("LAN Server error")
                }
            }
        }
    }

    private fun registerService(nickname: String) {
        val portToUse = serverSocket?.localPort ?: return
        if (portToUse <= 0) {
            Log.e("LanTransport", "Invalid port for registration: $portToUse")
            return
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$nickname|$myNodeId"
            serviceType = SERVICE_TYPE
            port = portToUse
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("LanTransport", "Nsd registration failed", e)
        }
    }

    private fun discoverServices() {
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("LanTransport", "Discovery start failed", e)
        }
    }

    override fun stop() {
        try { nsdManager.unregisterService(registrationListener) } catch (e: Exception) {}
        try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
        connectedSockets.values.forEach {
            try { it.close() } catch (e: Exception) {}
        }
        connectedSockets.clear()
        nodeIdToName.clear()
        transportScope.cancel()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray(Charsets.UTF_8)
        transportScope.launch {
            try {
                if (endpointId != null) {
                    val socket = connectedSockets[endpointId]
                    if (socket != null && !socket.isClosed) {
                        writeToSocket(socket, data)
                    }
                } else {
                    connectedSockets.forEach { (id, socket) ->
                        if (!socket.isClosed) {
                            try {
                                writeToSocket(socket, data)
                            } catch (e: Exception) {
                                connectedSockets.remove(id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LanTransport", "Send failed", e)
            }
        }
    }

    private fun writeToSocket(socket: Socket, data: ByteArray) {
        synchronized(socket.outputStream) {
            val dos = java.io.DataOutputStream(socket.outputStream)
            dos.writeInt(data.size)
            dos.write(data)
            dos.flush()
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        val hostAddress = socket.inetAddress.hostAddress ?: "unknown"
        val endpointId = hostAddress
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = context.getString(R.string.lan_peer_name, hostAddress)
        callback.onConnectionChanged(nodeIdToName.toMap())

        transportScope.launch {
            try {
                val dis = java.io.DataInputStream(socket.getInputStream())
                while (isActive && !socket.isClosed) {
                    val length = try { dis.readInt() } catch (e: Exception) { break }
                    if (length > 1024 * 1024) throw IOException("Packet too large: $length")
                    val payload = ByteArray(length)
                    dis.readFully(payload)
                    val json = String(payload, Charsets.UTF_8)
                    callback.onPacketReceived(endpointId, json)
                }
            } catch (e: java.io.EOFException) {
                // Normal
            } catch (e: IOException) {
                // Lost
            } catch (e: Exception) {
                Log.e("LanTransport", "Error reading from socket", e)
            } finally {
                connectedSockets.remove(endpointId)
                nodeIdToName.remove(endpointId)
                callback.onConnectionChanged(nodeIdToName.toMap())
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(p0: NsdServiceInfo?) {}
        override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {}
        override fun onServiceUnregistered(p0: NsdServiceInfo?) {}
        override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(p0: String?) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType == SERVICE_TYPE) {
                // Ignore self
                if (service.serviceName.contains(myNodeId)) {
                    return
                }

                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
                        Log.e("LanTransport", "Resolve failed: $p1")
                    }
                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            resolvedService.hostAddresses.firstOrNull()
                        } else {
                            @Suppress("DEPRECATION")
                            resolvedService.host
                        }

                        if (host != null) {
                            transportScope.launch {
                                try {
                                    val address = host.hostAddress
                                    if (address != null && !connectedSockets.containsKey(address)) {
                                        val socket = Socket(host, resolvedService.port)
                                        handleIncomingSocket(socket)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LanTransport", "Failed to connect to resolved service", e)
                                }
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    nsdManager.resolveService(service, Dispatchers.IO.asExecutor(), resolveListener)
                } else {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(service, resolveListener)
                }
            }
        }
        override fun onServiceLost(p0: NsdServiceInfo?) {}
        override fun onDiscoveryStopped(p0: String?) {}
        override fun onStartDiscoveryFailed(p0: String?, p1: Int) {}
        override fun onStopDiscoveryFailed(p0: String?, p1: Int) {}
    }
}
