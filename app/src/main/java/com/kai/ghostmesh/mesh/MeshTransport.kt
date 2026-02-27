package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.model.Packet

interface MeshTransport {
    fun start(nickname: String, isStealth: Boolean)
    fun stop()
    fun sendPacket(packet: Packet, endpointId: String? = null)
    
    interface Callback {
        fun onPacketReceived(endpointId: String, json: String)
        fun onConnectionChanged(nodes: Map<String, String>)
        fun onError(message: String)
    }
}
