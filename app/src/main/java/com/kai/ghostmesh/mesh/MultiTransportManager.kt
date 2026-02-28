package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.model.Packet
import java.util.concurrent.ConcurrentHashMap

class MultiTransportManager(
    private val transports: List<MeshTransport>,
    private val callback: MeshTransport.Callback
) : MeshTransport {

    private val allNodes = ConcurrentHashMap<String, String>() // nodeId to transportName:nodeId

    init {
        transports.forEach { transport ->
            val name = when (transport) {
                is GoogleNearbyTransport -> "Nearby"
                is BluetoothLegacyTransport -> "Bluetooth"
                is LanTransport -> "LAN"
                is WifiDirectTransport -> "WiFiDirect"
                else -> "Unknown"
            }
            transport.setCallback(createWrappedCallback(name))
        }
    }

    private fun createWrappedCallback(transportName: String) = object : MeshTransport.Callback {
        override fun onPacketReceived(endpointId: String, json: String) {
            callback.onPacketReceived("$transportName:$endpointId", json)
        }

        override fun onConnectionChanged(nodes: Map<String, String>) {
            // Update global nodes map
            val prefix = "$transportName:"
            // Remove old nodes from this transport
            allNodes.keys.removeIf { it.startsWith(prefix) }
            // Add new ones
            nodes.forEach { (id, name) ->
                allNodes["$prefix$id"] = name
            }
            callback.onConnectionChanged(allNodes.toMap())
        }

        override fun onError(message: String) {
            callback.onError("[$transportName] $message")
        }
    }

    override fun start(nickname: String, isStealth: Boolean) {
        transports.forEach { it.start(nickname, isStealth) }
    }

    override fun stop() {
        transports.forEach { it.stop() }
        allNodes.clear()
    }

    override fun setCallback(callback: MeshTransport.Callback) {
        // Callback is already handled per-transport in init
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        if (endpointId != null) {
            val parts = endpointId.split(":", limit = 2)
            if (parts.size == 2) {
                val transportName = parts[0]
                val actualId = parts[1]
                findTransport(transportName)?.sendPacket(packet, actualId)
            }
        } else {
            transports.forEach { it.sendPacket(packet) }
        }
    }

    private fun findTransport(name: String): MeshTransport? {
        return when (name) {
            "Nearby" -> transports.filterIsInstance<GoogleNearbyTransport>().firstOrNull()
            "Bluetooth" -> transports.filterIsInstance<BluetoothLegacyTransport>().firstOrNull()
            "LAN" -> transports.filterIsInstance<LanTransport>().firstOrNull()
            "WiFiDirect" -> transports.filterIsInstance<WifiDirectTransport>().firstOrNull()
            else -> null
        }
    }
}
