package com.kai.ghostmesh.core.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import com.kai.ghostmesh.core.util.GhostLog as Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

data class Route(
    val destinationId: String,
    val nextHopEndpointId: String,
    val cost: Float,
    val battery: Int,
    val failureRate: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val cacheSize: Int = AppConfig.PACKET_CACHE_SIZE,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String, Int, String?) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        const val CURRENT_PROTOCOL_VERSION = 1
        const val MAX_LATENCY_MS = 2000L
        const val MAX_FAILURE_RATE = 0.4f
    }

    private val engineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val processingChannel = Channel<Pair<String, String>>(Channel.UNLIMITED)

    init {
        repeat(2) {
            engineScope.launch {
                for ((endpoint, json) in processingChannel) {
                    processPacketInternal(endpoint, json)
                }
            }
        }
    }

    private val processedPackets = Collections.synchronizedMap(object : LinkedHashMap<String, Long>(cacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Long>?): Boolean {
            return size > cacheSize
        }
    })

    // Optimized Routing: Destination ID -> (NextHop ID -> Route) for O(1) lookups
    private val routingTable = ConcurrentHashMap<String, ConcurrentHashMap<String, Route>>()
    // Cache for best routes to ensure O(1) lookups for the primary path
    private val bestRoutesCache = ConcurrentHashMap<String, Route>()
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
        return bestRoutesCache.toMap()
    }

    fun processIncomingJson(fromEndpointId: String, json: String) {
        if (json.length > 102400) return
        processingChannel.trySend(fromEndpointId to json)
    }

    private fun processPacketInternal(fromEndpointId: String, json: String) {
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

        // OPTIMIZATION: Discard own packets immediately to save signature verification cycles
        if (packet.senderId == myNodeId) return

        if (packet.protocolVersion > CURRENT_PROTOCOL_VERSION) {
            Log.i("MeshEngine", "Future protocol version: ${packet.protocolVersion}")
        }

        val signature = packet.signature
        if (signature == null || !SecurityManager.verifyPacket(packet.senderId, packet.id, packet.payload, signature)) {
             return
        }

        if (!validatePacketSchema(packet)) {
            Log.e("MeshEngine", "Schema validation failed for type: ${packet.type}")
            return
        }

        if (processedPackets.containsKey(packet.id)) return
        processedPackets[packet.id] = System.currentTimeMillis()

        if (packet.type == PacketType.TUNNEL && packet.receiverId == myNodeId) {
            try {
                val innerPacket = gson.fromJson(packet.payload, Packet::class.java)
                processIncomingPacket(fromEndpointId, innerPacket)
            } catch (e: Exception) {
            }
            return
        }

        processIncomingPacket(fromEndpointId, packet)
    }

    private fun validatePacketSchema(packet: Packet): Boolean {
        return when (packet.type) {
            PacketType.PROFILE_SYNC -> {
                val parts = packet.payload.split("|")
                // Strict Schema: name [1-32], status [<=64], color
                parts.size == 3 && parts[0].length in 1..32 && parts[1].length <= 64 && parts[2].isNotBlank()
            }
            PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.VIDEO, PacketType.FILE -> packet.payload.isNotBlank()
            PacketType.ACK, PacketType.READ_RECEIPT -> {
                // UUID Standard Enforcement: 36 chars with 4 dashes
                packet.payload.length == 36 && packet.payload.count { it == '-' } == 4
            }
            PacketType.BATTERY_HEARTBEAT -> packet.payload.startsWith("Heartbeat|")
            PacketType.KEY_EXCHANGE -> packet.payload.isNotBlank()
            else -> true
        }
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
                PacketType.BATTERY_HEARTBEAT -> {
                    updateRoutingTable(packet.senderId, fromEndpointId, totalPathCost, packet.senderBattery)
                }
                PacketType.TUNNEL -> {}
                PacketType.READ_RECEIPT -> {
                    onHandlePacket(packet)
                }
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
                            signature = ackSignature,
                            protocolVersion = CURRENT_PROTOCOL_VERSION
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
        val nextHops = routingTable.getOrPut(destId) { ConcurrentHashMap<String, Route>() }
        val newRoute = Route(destId, nextHop, cost, battery)

        val existing = nextHops[nextHop]
        // Only update if cost improved significantly, battery changed, or entry is stale
        if (existing == null || cost < existing.cost || battery != existing.battery || System.currentTimeMillis() - existing.timestamp > 30000) {
            nextHops[nextHop] = newRoute
            updateBestRouteCache(destId)
        }
    }

    private fun updateBestRouteCache(destId: String) {
        val nextHops = routingTable[destId]
        if (nextHops == null || nextHops.isEmpty()) {
            bestRoutesCache.remove(destId)
            return
        }
        val bestRoute = nextHops.values.minWithOrNull(compareBy<Route> { it.cost }.thenByDescending { it.battery })
        if (bestRoute != null) {
            bestRoutesCache[destId] = bestRoute
        } else {
            bestRoutesCache.remove(destId)
        }
    }

    private fun relayPacket(packet: Packet, fromEndpointId: String?) {
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packet, fromEndpointId)
        } else {
            // Check best route cache first
            var bestRoute = bestRoutesCache[packet.receiverId]

            // If best route is the one we received from, we must find an alternative
            if (bestRoute?.nextHopEndpointId == fromEndpointId) {
                bestRoute = routingTable[packet.receiverId]?.values
                    ?.filter { it.nextHopEndpointId != fromEndpointId }
                    ?.minWithOrNull(compareBy<Route> { it.cost }.thenByDescending { it.battery })
            }

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
            .mapNotNull { id ->
                val bestRoute = routingTable[id]?.values?.minWithOrNull(compareBy<Route> { it.cost }.thenByDescending { it.battery })
                bestRoute?.let { id to it.cost }
            }
            .minByOrNull { it.second }
            ?.first ?: return

        val gatewayRoute = routingTable[bestGatewayId]?.values
            ?.minWithOrNull(compareBy<Route> { it.cost }.thenByDescending { it.battery })
            ?: return

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
            signature = tunnelSignature,
            protocolVersion = CURRENT_PROTOCOL_VERSION
        )
        onSendToNeighbors(tunnelPacket, gatewayRoute.nextHopEndpointId)
    }

    fun sendPacket(packet: Packet) {
        val packetWithBattery = packet.copy(
            senderBattery = myBattery,
            pathCost = 0f,
            protocolVersion = CURRENT_PROTOCOL_VERSION
        )
        if (packet.receiverId == "ALL") {
            onSendToNeighbors(packetWithBattery, null)
        } else {
            val bestRoute = bestRoutesCache[packet.receiverId]
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
            signature = signature,
            protocolVersion = CURRENT_PROTOCOL_VERSION
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
            val nextHops = entry.value

            var changed = false
            val innerIterator = nextHops.entries.iterator()
            while (innerIterator.hasNext()) {
                val route = innerIterator.next().value
                if (now - route.timestamp > AppConfig.ROUTE_PRUNE_TIMEOUT_MS || route.failureRate > MAX_FAILURE_RATE) {
                    innerIterator.remove()
                    changed = true
                }
            }

            if (nextHops.isEmpty()) {
                iterator.remove()
                bestRoutesCache.remove(entry.key)
                SecurityManager.removeSession(entry.key)
            } else if (changed) {
                updateBestRouteCache(entry.key)
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

    fun stop() {
        engineScope.cancel()
        processingChannel.close()
    }
}
