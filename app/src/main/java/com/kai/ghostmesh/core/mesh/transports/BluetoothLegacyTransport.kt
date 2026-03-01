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

    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val gson = Gson()
    private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val SERVICE_NAME = "GhostMesh"

    private val connectedSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val nodeIdToName = ConcurrentHashMap<String, String>()
    private val socketExecutor = java.util.concurrent.Executors.newCachedThreadPool()

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
        socketExecutor.shutdownNow()
        callback.onConnectionChanged(emptyMap())
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        val json = gson.toJson(packet)
        val data = json.toByteArray(Charsets.UTF_8)

        if (endpointId != null) {
            val socket = connectedSockets[endpointId]
            if (socket != null && socket.isConnected) {
                synchronized(socket.outputStream) {
                    val dos = java.io.DataOutputStream(socket.outputStream)
                    dos.writeInt(data.size)
                    dos.write(data)
                    dos.flush()
                }
            }
        } else {
            connectedSockets.values.forEach { socket ->
                if (socket.isConnected) {
                    synchronized(socket.outputStream) {
                        val dos = java.io.DataOutputStream(socket.outputStream)
                        dos.writeInt(data.size)
                        dos.write(data)
                        dos.flush()
                    }
                }
            }
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

        socketExecutor.execute {
            try {
                val dis = java.io.DataInputStream(socket.inputStream)
                while (true) {
                    val length = dis.readInt()
                    if (length > 1024 * 1024) throw IOException("Packet too large: $length")
                    val payload = ByteArray(length)
                    dis.readFully(payload)
                    val json = String(payload, Charsets.UTF_8)
                    callback.onPacketReceived(endpointId, json)
                }
            } catch (e: java.io.EOFException) {
                // Connection closed normally
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
}
