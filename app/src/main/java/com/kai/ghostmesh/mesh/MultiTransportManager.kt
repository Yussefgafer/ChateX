package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.model.Packet
import java.util.concurrent.ConcurrentHashMap

class MultiTransportManager(
    private val transports: List<MeshTransport>,
    private val callback: MeshTransport.Callback
) : MeshTransport {

    private val allNodes = ConcurrentHashMap<String, String>()

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
            val prefix = "$transportName:"
            allNodes.keys.removeIf { it.startsWith(prefix) }
            nodes.forEach { (id, name) -> allNodes["$prefix$id"] = name }
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

    override fun setCallback(callback: MeshTransport.Callback) {}

    override fun sendPacket(packet: Packet, endpointId: String?) {
        if (endpointId != null) {
            val parts = endpointId.split(":", limit = 2)
            if (parts.size == 2) findTransport(parts[0])?.sendPacket(packet, parts[1])
        } else {
            transports.forEach { it.sendPacket(packet) }
        }
    }

    private fun findTransport(name: String): MeshTransport? = when (name) {
        "Nearby" -> transports.filterIsInstance<GoogleNearbyTransport>().firstOrNull()
        "Bluetooth" -> transports.filterIsInstance<BluetoothLegacyTransport>().firstOrNull()
        "LAN" -> transports.filterIsInstance<LanTransport>().firstOrNull()
        "WiFiDirect" -> transports.filterIsInstance<WifiDirectTransport>().firstOrNull()
        else -> null
    }
}
