package com.kai.ghostmesh.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.kai.ghostmesh.model.Packet
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothLegacyTransport(
    private val context: Context,
    private val myNodeId: String,
    private var callback: MeshTransport.Callback
) : MeshTransport {

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val SERVICE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66") // Unique for ChateX
    private val NAME = "ChateX_Mesh"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null
    private val activeConnections = ConcurrentHashMap<String, BluetoothSocket>()
    private val discoveredDevices = ConcurrentHashMap<String, String>() // Address to Name

    override fun start(nickname: String, isStealth: Boolean) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            callback.onError("Bluetooth is disabled or not supported.")
            return
        }

        // Start listening for incoming connections
        startServer()
        
        if (!isStealth) {
            startDiscovery()
        }

        Log.d("LegacyTransport", "Started listening on UUID: $SERVICE_UUID")
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            while (isActive) {
                if (bluetoothAdapter?.isDiscovering == false) {
                    bluetoothAdapter.startDiscovery()
                }
                delay(12000) // Discovery is battery intensive
            }
        }

        // Note: Real implementation would need a BroadcastReceiver for BluetoothDevice.ACTION_FOUND
        // and BluetoothAdapter.ACTION_DISCOVERY_FINISHED to actually connect.
        // For now, we improve the structure.
    }

    private fun startServer() {
        serverJob?.cancel()
        serverJob = scope.launch {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, SERVICE_UUID)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleNewConnection(socket)
                }
            } catch (e: IOException) {
                Log.e("LegacyTransport", "Server Error", e)
            } finally {
                serverSocket?.close()
            }
        }
    }

    private fun handleNewConnection(socket: BluetoothSocket) {
        scope.launch {
            try {
                val input = socket.inputStream
                val output = socket.outputStream
                
                // Simple Handshake: Wait for NodeId
                val buffer = ByteArray(1024)
                val bytes = input.read(buffer)
                val peerNodeId = String(buffer, 0, bytes)
                
                activeConnections[peerNodeId] = socket
                Log.d("LegacyTransport", "Connected to peer: $peerNodeId")
                
                // Keep reading packets
                while (isActive) {
                    val pBytes = input.read(buffer)
                    if (pBytes == -1) break
                    val json = String(buffer, 0, pBytes)
                    callback.onPacketReceived(peerNodeId, json)
                }
            } catch (e: Exception) {
                Log.e("LegacyTransport", "Connection Error", e)
            } finally {
                socket.close()
            }
        }
    }

    override fun stop() {
        serverJob?.cancel()
        discoveryJob?.cancel()
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
        scope.cancel()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        scope.launch {
            try {
                val json = com.google.gson.Gson().toJson(packet)
                val bytes = json.toByteArray()
                
                if (endpointId != null) {
                    activeConnections[endpointId]?.outputStream?.write(bytes)
                } else {
                    activeConnections.values.forEach { it.outputStream.write(bytes) }
                }
            } catch (e: Exception) {
                Log.e("LegacyTransport", "Send Error", e)
            }
        }
    }
}
