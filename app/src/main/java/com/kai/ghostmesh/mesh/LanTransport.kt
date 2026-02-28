package com.kai.ghostmesh.mesh

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.kai.ghostmesh.model.Packet
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class LanTransport(
    private val context: Context,
    private val myNodeId: String,
    private val callback: MeshTransport.Callback
) : MeshTransport {

    private val TAG = "LanTransport"
    private val SERVICE_TYPE = "_chatex._tcp."
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var serverSocket: ServerSocket? = null
    private var localPort: Int = 0
    private val activeConnections = ConcurrentHashMap<String, Socket>()
    private val nodeIdToAddress = ConcurrentHashMap<String, String>()

    override fun start(nickname: String, isStealth: Boolean) {
        scope.launch {
            try {
                serverSocket = ServerSocket(0).also { localPort = it.localPort }
                if (!isStealth) {
                    registerService(nickname)
                }
                discoverServices()
                listenForConnections()
            } catch (e: Exception) {
                callback.onError("LAN Start Error: ${e.message}")
            }
        }
    }

    private fun registerService(nickname: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$nickname|$myNodeId"
            serviceType = SERVICE_TYPE
            setPort(localPort)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun listenForConnections() {
        scope.launch {
            while (isActive) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingSocket(socket)
                } catch (e: Exception) {
                    Log.e(TAG, "Accept error", e)
                }
            }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        scope.launch {
            try {
                val input = socket.inputStream
                val buffer = ByteArray(4096)
                while (isActive) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    val json = String(buffer, 0, bytesRead)
                    // In a real impl, we'd handshake for NodeID first
                    callback.onPacketReceived(socket.inetAddress.hostAddress ?: "unknown", json)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket read error", e)
            } finally {
                socket.close()
            }
        }
    }

    override fun stop() {
        try {
            nsdManager.unregisterService(registrationListener)
            nsdManager.stopServiceDiscovery(discoveryListener)
            serverSocket?.close()
            activeConnections.values.forEach { it.close() }
            activeConnections.clear()
            scope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Stop error", e)
        }
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        scope.launch {
            val json = com.google.gson.Gson().toJson(packet)
            val bytes = json.toByteArray()
            if (endpointId != null) {
                activeConnections[endpointId]?.outputStream?.write(bytes)
            } else {
                activeConnections.values.forEach { it.outputStream.write(bytes) }
            }
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType == SERVICE_TYPE && !service.serviceName.contains(myNodeId)) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        connectToService(serviceInfo)
                    }
                })
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private fun connectToService(service: NsdServiceInfo) {
        scope.launch {
            try {
                val socket = Socket(service.host, service.port)
                val peerNodeId = service.serviceName.split("|").getOrNull(1) ?: service.host.hostAddress
                activeConnections[peerNodeId] = socket
                handleIncomingSocket(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Connect error", e)
            }
        }
    }
}
