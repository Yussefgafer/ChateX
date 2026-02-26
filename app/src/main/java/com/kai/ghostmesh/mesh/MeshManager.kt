package com.kai.ghostmesh.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import java.nio.charset.StandardCharsets

class MeshManager(
    private val context: Context,
    private val myNodeId: String,
    private val onPacketReceived: (Packet) -> Unit,
    private val onConnectionChanged: (Map<String, String>) -> Unit
) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.kai.chatex.SERVICE_ID"

    private val connectedEndpoints = mutableSetOf<String>()
    private val endpointIdToNodeId = mutableMapOf<String, String>() // Map NearbyId to PacketNodeId
    private val nodeIdToName = mutableMapOf<String, String>()
    
    // Packet Cache to prevent loops
    private val processedPacketIds = mutableSetOf<String>()

    fun startMesh(nickname: String) {
        val optionsAdv = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(nickname, SERVICE_ID, connectionLifecycleCallback, optionsAdv)
        
        val optionsDisc = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, optionsDisc)
    }

    fun sendPacket(packet: Packet) {
        processedPacketIds.add(packet.id) // Don't process my own packet if it comes back
        val json = gson.toJson(packet)
        val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
        
        // Broadcast to all immediate neighbors
        for (endpointId in connectedEndpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        nodeIdToName.clear()
        onConnectionChanged(emptyMap())
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                // We'll get the NodeId once they send their first PROFILE_SYNC packet
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            val nodeId = endpointIdToNodeId.remove(endpointId)
            if (nodeId != null) {
                nodeIdToName.remove(nodeId)
                onConnectionChanged(nodeIdToName.toMap())
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(myNodeId, endpointId, connectionLifecycleCallback)
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                val json = String(it, StandardCharsets.UTF_8)
                val packet = gson.fromJson(json, Packet::class.java)
                
                handleIncomingPacket(endpointId, packet)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleIncomingPacket(fromEndpointId: String, packet: Packet) {
        if (processedPacketIds.contains(packet.id)) return // Deduplication
        processedPacketIds.add(packet.id)

        // Update routing table
        endpointIdToNodeId[fromEndpointId] = packet.senderId
        nodeIdToName[packet.senderId] = packet.senderName
        onConnectionChanged(nodeIdToName.toMap())

        // Logic: Process or Relay?
        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        
        if (isForMe) {
            onPacketReceived(packet)
        }

        // Relay Logic (Mesh Core)
        if (packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)) {
            val relayPacket = packet.copy(hopCount = packet.hopCount - 1)
            val json = gson.toJson(relayPacket)
            val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
            
            for (neighbor in connectedEndpoints) {
                if (neighbor != fromEndpointId) { // Don't send back to sender
                    connectionsClient.sendPayload(neighbor, payload)
                }
            }
        }
    }
}
