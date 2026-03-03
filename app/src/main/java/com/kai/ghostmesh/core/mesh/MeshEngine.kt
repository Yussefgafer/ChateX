package com.kai.ghostmesh.core.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import com.kai.ghostmesh.core.util.GhostLog as Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

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
    private val cacheSize: Int = AppConfig.PACKET_CACHE_SIZE,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String, Int, String?) -> Unit
) {
    private val processedPackets = Collections.synchronizedMap(object : LinkedHashMap<String, Long>(cacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Long>?): Boolean {
            return size > cacheSize
        }
    })

    private val routingTable = ConcurrentHashMap<String, CopyOnWriteArrayList<Route>>()
    private val gatewayNodes = ConcurrentHashMap<String, Long>()
    private val gson = Gson()
    private var myBattery: Int = 100
    private var isStealth: Boolean = false
    private var lastPruneTime = 0L

    fun updateMyBattery(battery: Int) {
        myBattery = battery
    }

    fun setStealth(stealth: Boolean) {
        this.isStealth = stealth
    }

    fun getRoutingTable(): Map<String, Route> {
        return routingTable.mapNotNull { entry ->
            entry.value.firstOrNull()?.let { entry.key to it }
        }.toMap()
    }

    fun processIncomingJson(fromEndpointId: String, json: String) {
        if (json.length > 102400) return

        val now = System.currentTimeMillis()
        if (now - lastPruneTime > 60000) {
            pruneGateways()
            pruneRoutes()
            pruneProcessedPackets()
            lastPruneTime = now
        }

        val packet = try {
            val p = gson.fromJson(json, Packet::class.java)
            if (p != null && p.isValid()) p else null
        } catch (e: Exception) { null } ?: return

        val signature = packet.signature
        if (signature == null || !SecurityManager.verifyPacket(packet.senderId, packet.id, packet.payload, signature)) {
             Log.e("MeshEngine", "Invalid or missing packet signature from " + packet.senderId)
             return
        }

        if (processedPackets.containsKey(packet.id)) return
        processedPackets[packet.id] = System.currentTimeMillis()

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
        val transport = fromEndpointId.split(":").firstOrNull() ?: "Unknown"
        val linkCost = calculateLinkCost(transport, packet.senderBattery)
        val totalPathCost = packet.pathCost + linkCost

        updateRoutingTable(packet.senderId, fromEndpointId, totalPathCost, packet.senderBattery)

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
                PacketType.TUNNEL -> {}
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.VIDEO, PacketType.FILE,
                PacketType.ACK, PacketType.TYPING_START, PacketType.TYPING_STOP,
                PacketType.REACTION, PacketType.KEY_EXCHANGE, PacketType.LAST_SEEN,
                PacketType.PROFILE_IMAGE -> {
                    onHandlePacket(packet)
                    
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

        val shouldRelay = packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)
        if (shouldRelay) {
            val relayedPacket = packet.copy(
                hopCount = packet.hopCount - 1,
                pathCost = totalPathCost,
                senderBattery = myBattery
            )
            relayPacket(relayedPacket, fromEndpointId)
        }
    }

    private fun updateRoutingTable(destId: String, nextHop: String, cost: Float, battery: Int) {
        val routes = routingTable.getOrPut(destId) { CopyOnWriteArrayList<Route>() }
        val newRoute = Route(destId, nextHop, cost, battery)

        synchronized(routes) {
            val existingIndex = routes.indexOfFirst { it.nextHopEndpointId == nextHop }
            if (existingIndex != -1) {
                val existing = routes[existingIndex]
                if (cost < existing.cost || System.currentTimeMillis() - existing.timestamp > 30000) {
                    routes[existingIndex] = newRoute
                } else {
                    return
                }
            } else {
                routes.add(newRoute)
            }

            if (routes.size > 1) {
                val sorted = routes.sortedBy { it.cost }.take(3)
                routes.clear()
                routes.addAll(sorted)
            }
        }
    }

    private fun relayPacket(packet: Packet, fromEndpointId: String?) {
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packet, fromEndpointId)
        } else {
            val bestRoute = routingTable[packet.receiverId]?.firstOrNull { it.nextHopEndpointId != fromEndpointId }
            if (bestRoute != null) {
                onSendToNeighbors(packet, bestRoute.nextHopEndpointId)
            } else if (gatewayNodes.isNotEmpty()) {
                tunnelToGateway(packet)
            } else {
                onSendToNeighbors(packet, fromEndpointId)
            }
        }
    }

    private fun tunnelToGateway(packet: Packet) {
        val bestGatewayId = gatewayNodes.keys()
            .toList()
            .mapNotNull { id -> routingTable[id]?.firstOrNull()?.let { id to it.cost } }
            .minByOrNull { it.second }
            ?.first ?: return

        val gatewayRoute = routingTable[bestGatewayId]?.firstOrNull() ?: return

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
            val bestRoute = routingTable[packet.receiverId]?.firstOrNull()
            if (bestRoute != null) {
                onSendToNeighbors(packetWithBattery, bestRoute.nextHopEndpointId)
            } else if (gatewayNodes.isNotEmpty()) {
                tunnelToGateway(packetWithBattery)
            } else {
                onSendToNeighbors(packetWithBattery, null)
            }
        }
    }

    fun generateHeartbeat(): Packet {
        val packetId = java.util.UUID.randomUUID().toString()
        val payload = "Heartbeat|${System.currentTimeMillis()}"
        val signature = SecurityManager.signPacket(packetId, payload)
        return Packet(
            id = packetId,
            senderId = myNodeId,
            senderName = myNickname,
            receiverId = "ALL",
            type = PacketType.BATTERY_HEARTBEAT,
            payload = payload,
            senderBattery = myBattery,
            signature = signature
        )
    }

    private fun calculateLinkCost(transport: String, battery: Int): Float {
        var cost = when (transport) {
            "LAN" -> 1f
            "WiFiDirect" -> 2f
            "Nearby" -> 5f
            "Bluetooth" -> 10f
            "Cloud" -> 50f
            else -> 15f
        }
        if (battery < 15) cost *= 5f
        else if (battery < 30) cost *= 2f
        return cost
    }

    private fun pruneGateways() {
        val now = System.currentTimeMillis()
        val iterator = gatewayNodes.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > AppConfig.GATEWAY_PRUNE_TIMEOUT_MS) {
                iterator.remove()
                SecurityManager.removeSession(entry.key)
            }
        }
    }

    private fun pruneRoutes() {
        val now = System.currentTimeMillis()
        val iterator = routingTable.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val routes = entry.value
            synchronized(routes) {
                routes.removeAll { now - it.timestamp > AppConfig.ROUTE_PRUNE_TIMEOUT_MS }
                if (routes.isEmpty()) {
                    iterator.remove()
                    SecurityManager.removeSession(entry.key)
                }
            }
        }
    }

    private fun pruneProcessedPackets() {
        val now = System.currentTimeMillis()
        synchronized(processedPackets) {
            val iterator = processedPackets.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > AppConfig.PACKET_CACHE_TIMEOUT_MS) {
                    iterator.remove()
                }
            }
        }
    }

    private fun shouldAck(type: PacketType): Boolean = when(type) {
        PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.VIDEO, PacketType.FILE -> true
        else -> false
    }
}
