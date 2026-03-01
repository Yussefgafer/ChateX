package com.kai.ghostmesh.core.mesh

import com.kai.ghostmesh.core.model.Packet
import java.util.concurrent.ConcurrentHashMap

class MultiTransportManager(
    private val callback: MeshTransport.Callback
) : MeshTransport {

    override val name: String = "MultiTransport"
    private val transports = mutableListOf<MeshTransport>()
    private val allNodes = ConcurrentHashMap<String, String>()

    fun registerTransport(transport: MeshTransport) {
        transports.add(transport)
        transport.setCallback(createWrappedCallback(transport.name))
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

    override fun setCallback(callback: MeshTransport.Callback) {
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        if (endpointId != null) {
            val parts = endpointId.split(":", limit = 2)
            if (parts.size == 2) {
                findTransport(parts[0])?.sendPacket(packet, parts[1])
            } else {
                transports.forEach { it.sendPacket(packet, endpointId) }
            }
        } else {
            transports.forEach { it.sendPacket(packet) }
        }
    }

    private fun findTransport(name: String): MeshTransport? =
        transports.find { it.name == name }
}
