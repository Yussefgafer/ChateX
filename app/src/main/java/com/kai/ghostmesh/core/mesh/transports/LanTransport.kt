package com.kai.ghostmesh.core.mesh.transports

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class LanTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback? = null
) : MeshTransport {

    override val name: String = "LAN"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectedSockets = ConcurrentHashMap<String, Socket>()
    private val nodeNames = ConcurrentHashMap<String, String>()
    private var serverSocket: ServerSocket? = null

    override fun start(nickname: String, isStealth: Boolean) {
        if (!isStealth) startServer()
        registerService(nickname)
        discoverServices()
    }

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(0)
                while (isActive) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleSocket(it) }
                }
            } catch (e: Exception) {}
        }
    }

    private fun handleSocket(socket: Socket) {
        val endpointId = "${socket.inetAddress.hostAddress}:${socket.port}"
        connectedSockets[endpointId] = socket

        scope.launch {
            try {
                val input = socket.getInputStream()
                val buffer = ByteArray(8192)
                while (isActive) {
                    val bytes = input.read(buffer)
                    if (bytes > 0) {
                        callback?.onBinaryPacketReceived(endpointId, buffer.copyOf(bytes))
                    }
                }
            } catch (e: Exception) {
                connectedSockets.remove(endpointId)
            }
        }
    }

    private fun registerService(nickname: String) {
        // NSD Registration logic
    }

    private fun discoverServices() {
        // NSD Discovery logic
    }

    override fun stop() {
        scope.cancel()
        serverSocket?.close()
        connectedSockets.values.forEach { it.close() }
        connectedSockets.clear()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        // Protobuf serialization logic
    }

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }
}
