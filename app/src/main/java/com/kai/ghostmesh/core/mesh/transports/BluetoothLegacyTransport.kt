package com.kai.ghostmesh.core.mesh.transports

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import com.kai.ghostmesh.core.util.GhostLog as Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

@SuppressLint("MissingPermission")
class BluetoothLegacyTransport(
    override val name: String = "Bluetooth",
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val gson = Gson()
    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val SERVICE_NAME = "GhostMesh"

    private val connectedSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val nodeIdToName = ConcurrentHashMap<String, String>()
    private val transportScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun start(nickname: String, isStealth: Boolean) {
        if (bluetoothAdapter == null) {
            callback.onError("Bluetooth not supported")
            return
        }
        if (!isStealth) {
            startAcceptLoop()
        }
    }

    private fun startAcceptLoop() {
        transportScope.launch {
            val serverSocket: BluetoothServerSocket? = try {
                bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            } catch (e: Exception) {
                Log.e("Bluetooth", "Listen failed", e)
                null
            }

            try {
                while (isActive) {
                    val socket = try { serverSocket?.accept() } catch (e: IOException) { null }
                    socket?.let { handleConnectedSocket(it) }
                }
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    override fun stop() {
        connectedSockets.values.forEach { try { it.close() } catch (e: Exception) {} }
        connectedSockets.clear()
        nodeIdToName.clear()
        transportScope.cancel()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray(Charsets.UTF_8)

        transportScope.launch {
            if (endpointId != null) {
                val socket = connectedSockets[endpointId]
                if (socket != null && socket.isConnected) {
                    try {
                        writeToSocket(socket, data)
                    } catch (e: Exception) {
                        connectedSockets.remove(endpointId)
                    }
                }
            } else {
                connectedSockets.forEach { (id, socket) ->
                    if (socket.isConnected) {
                        try {
                            writeToSocket(socket, data)
                        } catch (e: Exception) {
                            connectedSockets.remove(id)
                        }
                    }
                }
            }
        }
    }

    private fun writeToSocket(socket: BluetoothSocket, data: ByteArray) {
        synchronized(socket.outputStream) {
            val dos = java.io.DataOutputStream(socket.outputStream)
            dos.writeInt(data.size)
            dos.write(data)
            dos.flush()
        }
    }

    private fun handleConnectedSocket(socket: BluetoothSocket) {
        val remoteDevice = socket.remoteDevice
        val endpointId = remoteDevice.address
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = remoteDevice.name ?: "Unknown Bluetooth"
        callback.onConnectionChanged(nodeIdToName.toMap())

        transportScope.launch {
            try {
                val dis = java.io.DataInputStream(socket.inputStream)
                while (isActive && socket.isConnected) {
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
                Log.e("Bluetooth", "Error reading from socket", e)
            } finally {
                connectedSockets.remove(endpointId)
                nodeIdToName.remove(endpointId)
                callback.onConnectionChanged(nodeIdToName.toMap())
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }
}
