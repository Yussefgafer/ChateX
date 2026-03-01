package com.kai.ghostmesh.core.mesh.transports

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothLegacyTransport(
    override val name: String = "Bluetooth",
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val gson = Gson()
    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val SERVICE_NAME = "GhostMesh"

    private val connectedSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val nodeIdToName = ConcurrentHashMap<String, String>()

    private var serverThread: AcceptThread? = null

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    override fun start(nickname: String, isStealth: Boolean) {
        if (bluetoothAdapter == null) {
            callback.onError("Bluetooth not supported")
            return
        }
        if (!isStealth) {
            serverThread = AcceptThread()
            serverThread?.start()
        }
        // Discovery is usually handled via system UI or a periodic scan
    }

    override fun stop() {
        serverThread?.cancel()
        connectedSockets.values.forEach { try { it.close() } catch (e: Exception) {} }
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

    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)

        override fun run() {
            while (true) {
                val socket = try { serverSocket?.accept() } catch (e: IOException) { break }
                socket?.let { handleConnectedSocket(it) }
            }
        }

        fun cancel() { try { serverSocket?.close() } catch (e: IOException) {} }
    }

    private fun handleConnectedSocket(socket: BluetoothSocket) {
        val endpointId = socket.remoteDevice.address
        connectedSockets[endpointId] = socket
        nodeIdToName[endpointId] = socket.remoteDevice.name ?: "Unknown Bluetooth"
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
}
