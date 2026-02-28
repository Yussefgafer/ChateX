package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger

class MeshNetworkStressTest {
    private val gson = Gson()
    private val myNodeId = "STRESS_NODE"
    private val myNickname = "HardenedNode"

    @Test
    fun `test handling high latency and packet loss simulation`() = runTest {
        val relayedCount = AtomicInteger(0)
        val handledCount = AtomicInteger(0)

        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { _, _ -> relayedCount.incrementAndGet() },
            onHandlePacket = { handledCount.incrementAndGet() },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        // Simulate 100 packets, 30% packet loss handled by the transport layer
        // (here we test if MeshEngine handles what DOES arrive correctly)
        val totalPackets = 100
        for (i in 1..totalPackets) {
            val packet = Packet(
                id = "PKT_$i",
                senderId = "SENDER_$i",
                senderName = "User",
                receiverId = myNodeId,
                type = PacketType.CHAT,
                payload = "Data $i"
            )
            engine.processIncomingJson("EP_$i", gson.toJson(packet))
        }

        assertEquals("Engine should handle all arriving packets", totalPackets, handledCount.get())
        // Each direct message to me should trigger an ACK
        assertEquals("Each direct message should trigger an ACK", totalPackets, relayedCount.get())
    }

    @Test
    fun `test multi-hop failure simulation`() {
        val relayedPackets = mutableListOf<Packet>()
        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        // Packet with hopCount 1 (last hop)
        val packet = Packet(
            id = "LAST_HOP",
            senderId = "A",
            senderName = "A",
            receiverId = "C",
            type = PacketType.CHAT,
            payload = "Final Stretch",
            hopCount = 1
        )

        engine.processIncomingJson("EP_A", gson.toJson(packet))

        assertEquals("Should relay with hopCount 0", 1, relayedPackets.size)
        assertEquals(0, relayedPackets[0].hopCount)

        // Packet with hopCount 0 should NOT be relayed
        relayedPackets.clear()
        val deadPacket = packet.copy(id = "DEAD", hopCount = 0)
        engine.processIncomingJson("EP_A", gson.toJson(deadPacket))

        assertTrue("Packet with hopCount 0 should not be relayed", relayedPackets.isEmpty())
    }
}
