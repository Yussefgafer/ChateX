package com.kai.ghostmesh.core.mesh.transports

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

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

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun start(nickname: String, isStealth: Boolean) {
        startServer()
        if (!isStealth) {
            registerService(nickname)
        }
        discoverServices()
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(0)
                while (true) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleIncomingSocket(it) }
                }
            } catch (e: IOException) {
                callback.onError("LAN Server error: ${e.message}")
            }
        }.start()
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
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stop() {
        try { nsdManager.unregisterService(registrationListener) } catch (e: Exception) {}
        try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
        serverSocket?.close()
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
        nodeIdToName[endpointId] = "LAN Peer"
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
                        val socket = Socket(resolvedService.host, resolvedService.port)
                        handleIncomingSocket(socket)
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
