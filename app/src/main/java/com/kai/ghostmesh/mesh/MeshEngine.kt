package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
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
    private val cacheSize: Int = 2000,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String, Int, String?) -> Unit
) {
    // O(1) packet deduplication with LRU eviction
    private val processedPacketIds: MutableSet<String> = Collections.newSetFromMap(
        object : LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>?): Boolean {
                return size > cacheSize
            }
        }
    )

    private val routingTable = ConcurrentHashMap<String, Route>()
    private val gson = Gson()
    private var myBattery: Int = 100

    fun updateMyBattery(battery: Int) {
        myBattery = battery
    }

    fun getRoutingTable(): Map<String, Route> = routingTable.toMap()

    fun processIncomingJson(fromEndpointId: String, json: String) {
        val packet = try {
            gson.fromJson(json, Packet::class.java)
        } catch (e: Exception) { return } ?: return

        // Deduplication
        if (!processedPacketIds.add(packet.id)) return

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
                PacketType.LINK_STATE -> {
                    // Logic to handle topology updates if needed
                }
                PacketType.BATTERY_HEARTBEAT -> {
                    // Already handled by routing table update above
                }
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE,
                PacketType.ACK, PacketType.TYPING_START, PacketType.TYPING_STOP,
                PacketType.REACTION, PacketType.KEY_EXCHANGE, PacketType.LAST_SEEN,
                PacketType.PROFILE_IMAGE -> {
                    onHandlePacket(packet)
                    
                    // Auto-ACK for direct messages
                    if (packet.receiverId != "ALL" && shouldAck(packet.type)) {
                        sendPacket(Packet(
                            senderId = myNodeId, senderName = myNickname, receiverId = packet.senderId,
                            type = PacketType.ACK, payload = packet.id
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

            if (packet.receiverId == "ALL") {
                onSendToNeighbors(relayedPacket, fromEndpointId)
            } else {
                val route = routingTable[packet.receiverId]
                if (route != null && route.nextHopEndpointId != fromEndpointId) {
                    // Unicast to the best known next hop
                    onSendToNeighbors(relayedPacket.copy(receiverId = packet.receiverId), route.nextHopEndpointId)
                } else {
                    // Fallback to broadcast if route unknown
                    onSendToNeighbors(relayedPacket, fromEndpointId)
                }
            }
        }
    }

    fun sendPacket(packet: Packet) {
        val packetWithBattery = packet.copy(senderBattery = myBattery, pathCost = 0f)
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packetWithBattery, null)
        } else {
            val route = routingTable[packet.receiverId]
            if (route != null) {
                onSendToNeighbors(packetWithBattery, route.nextHopEndpointId)
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
            else -> 15f
        }

        // Battery penalty
        if (battery < 15) cost *= 5f
        else if (battery < 30) cost *= 2f

        return cost
    }

    private fun shouldAck(type: PacketType): Boolean = when(type) {
        PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> true
        else -> false
    }
}
