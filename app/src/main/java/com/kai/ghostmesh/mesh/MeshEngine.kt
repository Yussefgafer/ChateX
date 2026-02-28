package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import java.util.Collections

class MeshEngine(
    private val myNodeId: String,
    private val myNickname: String,
    private val cacheSize: Int = 2000,
    private val onSendToNeighbors: (Packet, exceptEndpoint: String?) -> Unit,
    private val onHandlePacket: (Packet) -> Unit,
    private val onProfileUpdate: (String, String, String) -> Unit
) {
    // O(1) packet deduplication with LRU eviction
    private val processedPacketIds: MutableSet<String> = Collections.newSetFromMap(
        object : LinkedHashMap<String, Boolean>(cacheSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>?): Boolean {
                return size > cacheSize
            }
        }
    )

    private val gson = Gson()

    fun processIncomingJson(fromEndpointId: String, json: String) {
        val packet = try {
            gson.fromJson(json, Packet::class.java)
        } catch (e: Exception) { return } ?: return

        // Deduplication
        if (!processedPacketIds.add(packet.id)) return

        val isForMe = packet.receiverId == myNodeId || packet.receiverId == "ALL"
        
        if (isForMe) {
            when (packet.type) {
                PacketType.PROFILE_SYNC -> {
                    val parts = packet.payload.split("|")
                    onProfileUpdate(packet.senderId, parts.getOrNull(0) ?: "Unknown", parts.getOrNull(1) ?: "")
                }
                PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE,
                PacketType.ACK, PacketType.TYPING_START, PacketType.TYPING_STOP,
                PacketType.REACTION, PacketType.KEY_EXCHANGE, PacketType.LAST_SEEN,
                PacketType.PROFILE_IMAGE -> {
                    onHandlePacket(packet)
                    
                    // Auto-ACK for direct messages
                    if (packet.receiverId != "ALL" && shouldAck(packet.type)) {
                        val ack = Packet(
                            senderId = myNodeId, senderName = myNickname, receiverId = packet.senderId,
                            type = PacketType.ACK, payload = packet.id
                        )
                        onSendToNeighbors(ack, null)
                    }
                }
            }
        }

        // Multi-hop routing with loop prevention
        val shouldRelay = packet.hopCount > 0 && (packet.receiverId == "ALL" || packet.receiverId != myNodeId)
        if (shouldRelay) {
            onSendToNeighbors(packet.copy(hopCount = packet.hopCount - 1), fromEndpointId)
        }
    }

    private fun shouldAck(type: PacketType): Boolean = when(type) {
        PacketType.CHAT, PacketType.IMAGE, PacketType.VOICE, PacketType.FILE -> true
        else -> false
    }
}
