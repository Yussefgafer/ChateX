package com.kai.ghostmesh.core.mesh

import com.kai.ghostmesh.core.model.Packet

interface MeshTransport {
    val name: String
    fun start(nickname: String, isStealth: Boolean)
    fun stop()
    fun sendPacket(packet: Packet, endpointId: String? = null)
    fun setCallback(callback: Callback)
    
    interface Callback {
        fun onPacketReceived(endpointId: String, json: String)
        fun onConnectionChanged(nodes: Map<String, String>)
        fun onError(message: String)
    }
}
