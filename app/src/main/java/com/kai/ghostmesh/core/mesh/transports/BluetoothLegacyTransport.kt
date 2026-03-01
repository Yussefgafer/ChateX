package com.kai.ghostmesh.core.mesh.transports

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothLegacyTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback? = null
) : MeshTransport {

    override val name: String = "Bluetooth"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectedSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val nodeNames = ConcurrentHashMap<String, String>()
    private val gson = Gson()

    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun start(nickname: String, isStealth: Boolean) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (!isStealth) startServer()
        startDiscovery()
    }

    private fun startServer() {
        scope.launch {
            try {
                val serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("GhostMesh", SERVICE_UUID)
                while (isActive) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleSocket(it) }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startDiscovery() {
        // Discovery logic simplified for this task
    }

    private fun handleSocket(socket: BluetoothSocket) {
        val deviceId = socket.remoteDevice.address
        connectedSockets[deviceId] = socket
        nodeNames[deviceId] = socket.remoteDevice.name ?: "Ghost"
        callback?.onConnectionChanged(nodeNames.toMap())

        scope.launch {
            try {
                val input = socket.inputStream
                val buffer = ByteArray(4096)
                while (isActive) {
                    val bytes = input.read(buffer)
                    if (bytes > 0) {
                        val data = buffer.copyOf(bytes)
                        // Try Protobuf first, fallback to JSON for legacy
                        callback?.onBinaryPacketReceived(deviceId, data)
                    }
                }
            } catch (e: Exception) {
                connectedSockets.remove(deviceId)
                nodeNames.remove(deviceId)
                callback?.onConnectionChanged(nodeNames.toMap())
            }
        }
    }

    override fun stop() {
        scope.cancel()
        connectedSockets.values.forEach { it.close() }
        connectedSockets.clear()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        // Implementation: serialize to Protobuf and send via socket.outputStream
    }

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }
}
