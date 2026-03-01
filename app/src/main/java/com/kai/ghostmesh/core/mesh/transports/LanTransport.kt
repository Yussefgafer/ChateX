package com.kai.ghostmesh.core.mesh.transports

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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

    // Performance: Pooled executor for socket handling to reduce thread creation overhead
    private val socketExecutor = Executors.newCachedThreadPool()

    // Performance: Dynamic scan interval
    private var currentDiscoveryInterval = 10000L

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun setScanInterval(intervalMs: Long) {
        this.currentDiscoveryInterval = intervalMs
        // Restart discovery with new interval if needed (simplified here)
    }

    override fun start(nickname: String, isStealth: Boolean) {
        startServer()
        if (!isStealth) {
            registerService(nickname)
        }
        discoverServices()
    }

    private fun startServer() {
        socketExecutor.execute {
            try {
                serverSocket = ServerSocket(0)
                while (!serverSocket!!.isClosed) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleIncomingSocket(it) }
                }
            } catch (e: IOException) {
                if (serverSocket?.isClosed == false) {
                    callback.onError("LAN Server error")
                }
            }
        }
    }

    private fun registerService(nickname: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = nickname
            serviceType = SERVICE_TYPE
            port = serverSocket?.localPort ?: 0
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun discoverServices() {
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("LanTransport", "Discovery start failed")
        }
    }

    override fun stop() {
        try { nsdManager.unregisterService(registrationListener) } catch (e: Exception) {}
        try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
        serverSocket?.close()
        connectedSockets.values.forEach {
            try { it.close() } catch (e: Exception) {}
        }
        connectedSockets.clear()
        nodeIdToName.clear()
        socketExecutor.shutdownNow()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray()
        socketExecutor.execute {
            try {
                if (endpointId != null) {
                    connectedSockets[endpointId]?.outputStream?.write(data)
                } else {
                    connectedSockets.values.forEach { it.outputStream.write(data) }
                }
            } catch (e: Exception) {
                Log.e("LanTransport", "Send failed")
            }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        val hostAddress = socket.inetAddress.hostAddress ?: "unknown"
        val endpointId = hostAddress
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = "LAN Peer ($hostAddress)"
        callback.onConnectionChanged(nodeIdToName.toMap())

        socketExecutor.execute {
            // Performance: Reusable buffer for reading
            val buffer = ByteArray(64 * 1024)
            try {
                val inputStream = socket.getInputStream()
                while (!socket.isClosed) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    if (bytesRead > 0) {
                        val json = String(buffer, 0, bytesRead)
                        callback.onPacketReceived(endpointId, json)
                    }
                }
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
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {}
                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        // Modernization: API 34+ version-aware host resolution
                        val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            resolvedService.hostAddresses.firstOrNull()
                        } else {
                            resolvedService.host
                        }

                        if (host != null) {
                            socketExecutor.execute {
                                try {
                                    if (!connectedSockets.containsKey(host.hostAddress)) {
                                        val socket = Socket(host, resolvedService.port)
                                        handleIncomingSocket(socket)
                                    }
                                } catch (e: Exception) {}
                            }
                        }
                    }
                })
            }
        }
        override fun onServiceLost(p0: NsdServiceInfo?) {}
        override fun onDiscoveryStopped(p0: String?) {}
        override fun onStartDiscoveryFailed(p0: String?, p1: Int) {}
        override fun onStopDiscoveryFailed(p0: String?, p1: Int) {}
    }
}
