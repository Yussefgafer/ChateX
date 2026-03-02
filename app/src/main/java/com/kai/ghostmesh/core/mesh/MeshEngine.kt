package com.kai.ghostmesh.core.mesh

import com.kai.ghostmesh.core.util.GhostLog as Log
import com.google.gson.Gson
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.model.isValid
import com.kai.ghostmesh.core.security.SecurityManager
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class Route(
    val destinationId: String,
    val nextHopEndpointId: String,
    val cost: Float,
    val battery: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val cacheSize: Int = 300,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String, Int, String?) -> Unit
) {
    // O(1) packet deduplication with LRU eviction
    private val processedPacketIds: MutableSet<String> = Collections.synchronizedSet(Collections.newSetFromMap(
        object : LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>?): Boolean {
                return size > cacheSize
            }
        }
    ))

    private val routingTable = ConcurrentHashMap<String, Route>()
    private val gatewayNodes = ConcurrentHashMap<String, Long>() // nodeId to last heart beat
    private val gson = Gson()
    private var myBattery: Int = 100
    private var isStealth: Boolean = false

    fun updateMyBattery(battery: Int) {
        myBattery = battery
    }

    fun setStealth(stealth: Boolean) {
        this.isStealth = stealth
    }

    fun getRoutingTable(): Map<String, Route> = routingTable.toMap()

    fun processIncomingJson(fromEndpointId: String, json: String) {
        pruneGateways()
        pruneRoutes()
        // Security: Limit payload size to prevent OOM/Overflow attacks
        if (json.length > 102400) return

        val packet = try {
            val p = gson.fromJson(json, Packet::class.java)
            if (p != null && p.isValid()) p else null
        } catch (e: Exception) { null } ?: return

        // Signature verification (Mandatory)
        val signature = packet.signature
        if (signature == null || !SecurityManager.verifyPacket(packet.senderId, packet.id, packet.payload, signature)) {
             Log.e("MeshEngine", "Invalid or missing packet signature from " + packet.senderId)
             return
        }

        // Deduplication
        if (!processedPacketIds.add(packet.id)) return

        // Handle Tunnel Decapsulation
        if (packet.type == PacketType.TUNNEL && packet.receiverId == myNodeId) {
            try {
                val innerPacket = gson.fromJson(packet.payload, Packet::class.java)
                processIncomingPacket(fromEndpointId, innerPacket)
            } catch (e: Exception) {
                Log.e("MeshEngine", "Failed to decapsulate tunnel: ${e.message}")
            }
            return
        }

        processIncomingPacket(fromEndpointId, packet)
    }

    private fun processIncomingPacket(fromEndpointId: String, packet: Packet) {
        // Update Routing Table based on the incoming packet
        val transport = fromEndpointId.split(":").firstOrNull() ?: "Unknown"
        val linkCost = calculateLinkCost(transport, packet.senderBattery)
        val totalPathCost = packet.pathCost + linkCost

        val currentRoute = routingTable[packet.senderId]
        if (currentRoute == null || totalPathCost < currentRoute.cost || (System.currentTimeMillis() - currentRoute.timestamp > 30000)) {
            routingTable[packet.senderId] = Route(
                destinationId = packet.senderId,
                nextHopEndpointId = fromEndpointId,
                cost = totalPathCost,
                battery = packet.senderBattery
            )
        }

        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        
        if (isForMe) {
            when (packet.type) {
                PacketType.GATEWAY_AVAILABLE -> {
                    gatewayNodes[packet.senderId] = System.currentTimeMillis()
                    Log.d("MeshEngine", "Gateway detected: ${packet.senderId}")
                }
                PacketType.PROFILE_SYNC -> {
                    val parts = packet.payload.split("|")
                    onProfileUpdate(
                        packet.senderId,
                        parts.getOrNull(0) ?: "Unknown",
                        parts.getOrNull(1) ?: "",
                        packet.senderBattery,
                        fromEndpointId
                    )
                }
                PacketType.LINK_STATE -> {}
                PacketType.BATTERY_HEARTBEAT -> {}
                PacketType.TUNNEL -> { /* Already handled if targeted at me */ }
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE,
                PacketType.ACK, PacketType.TYPING_START, PacketType.TYPING_STOP,
                PacketType.REACTION, PacketType.KEY_EXCHANGE, PacketType.LAST_SEEN,
                PacketType.PROFILE_IMAGE -> {
                    onHandlePacket(packet)
                    
                    // Auto-ACK for direct messages
                    if (packet.receiverId != "ALL" && shouldAck(packet.type)) {
                        val ackPacketId = java.util.UUID.randomUUID().toString()
                        val ackPayload = packet.id
                        val ackSignature = SecurityManager.signPacket(ackPacketId, ackPayload)
                        sendPacket(Packet(
                            id = ackPacketId,
                            senderId = myNodeId, senderName = myNickname, receiverId = packet.senderId,
                            type = PacketType.ACK, payload = ackPayload,
                            signature = ackSignature
                        ))
                    }
                }
            }
        }

        // Multi-hop routing with intelligent switching
        val shouldRelay = packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)
        if (shouldRelay) {
            val relayedPacket = packet.copy(
                hopCount = packet.hopCount - 1,
                pathCost = totalPathCost,
                senderBattery = myBattery // Update with my battery for next hop
            )

            relayPacket(relayedPacket, fromEndpointId)
        }
    }

    private fun relayPacket(packet: Packet, fromEndpointId: String?) {
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packet, fromEndpointId)
        } else {
            val route = routingTable[packet.receiverId]
            if (route != null && route.nextHopEndpointId != fromEndpointId) {
                // Unicast to the best known next hop
                onSendToNeighbors(packet, route.nextHopEndpointId)
            } else if (gatewayNodes.isNotEmpty()) {
                // Not reachable via local mesh, but we have a gateway
                tunnelToGateway(packet)
            } else {
                // Fallback to broadcast if route unknown
                onSendToNeighbors(packet, fromEndpointId)
            }
        }
    }

    private fun tunnelToGateway(packet: Packet) {
        val bestGatewayId = gatewayNodes.keys().toList().firstOrNull() ?: return
        val gatewayRoute = routingTable[bestGatewayId] ?: return

        Log.d("MeshEngine", "Tunneling packet to Gateway ${bestGatewayId}")
        val tunnelPayload = gson.toJson(packet)
        val tunnelPacketId = java.util.UUID.randomUUID().toString()
        val tunnelSignature = SecurityManager.signPacket(tunnelPacketId, tunnelPayload)

        val tunnelPacket = Packet(
            id = tunnelPacketId,
            senderId = myNodeId,
            senderName = myNickname,
            receiverId = bestGatewayId,
            type = PacketType.TUNNEL,
            payload = tunnelPayload,
            hopCount = 3,
            signature = tunnelSignature
        )
        onSendToNeighbors(tunnelPacket, gatewayRoute.nextHopEndpointId)
    }

    fun sendPacket(packet: Packet) {
        val packetWithBattery = packet.copy(senderBattery = myBattery, pathCost = 0f)
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packetWithBattery, null)
        } else {
            val route = routingTable[packet.receiverId]
            if (route != null) {
                onSendToNeighbors(packetWithBattery, route.nextHopEndpointId)
            } else if (gatewayNodes.isNotEmpty()) {
                tunnelToGateway(packetWithBattery)
            } else {
                onSendToNeighbors(packetWithBattery, null)
            }
        }
    }

    private fun calculateLinkCost(transport: String, battery: Int): Float {
        var cost = when (transport) {
            "LAN" -> 1f
            "WiFiDirect" -> 2f
            "Nearby" -> 5f
            "Bluetooth" -> 10f
            "Cloud" -> 50f // High cost for cloud/virtual hop
            else -> 15f
        }

        // Battery penalty
        if (battery < 15) cost *= 5f
        else if (battery < 30) cost *= 2f

        return cost
    }

    private fun pruneGateways() {
        val now = System.currentTimeMillis()
        val iterator = gatewayNodes.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > 300000) { // 5 minutes
                iterator.remove()
                SecurityManager.removeSession(entry.key)
                Log.d("MeshEngine", "Pruned stale gateway: ${entry.key}")
            }
        }
    }

    private fun pruneRoutes() {
        val now = System.currentTimeMillis()
        val iterator = routingTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.timestamp > 600000) { // 10 minutes
                iterator.remove()
                SecurityManager.removeSession(entry.key)
                Log.d("MeshEngine", "Pruned stale route: ${entry.key}")
            }
        }
    }

    private fun shouldAck(type: PacketType): Boolean = when(type) {
        PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> true
        else -> false
    }
}
