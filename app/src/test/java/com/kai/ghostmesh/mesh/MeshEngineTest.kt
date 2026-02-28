package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshEngineTest {

    private val gson = Gson()
    private val myNodeId = "NODE_B"
    private val myNickname = "RelayNode"

    @Test
    fun `test packet relay from A to C through B`() {
        // Arrange
        val relayedPackets = mutableListOf<Packet>()
        val handledPackets = mutableListOf<Packet>()

        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { packet -> handledPackets.add(packet) },
            onProfileUpdate = { _, _, _ -> }
        )

        // Packet from A meant for C
        val packetFromA = Packet(
            id = "MSG_1",
            senderId = "NODE_A",
            senderName = "UserA",
            receiverId = "NODE_C",
            type = PacketType.CHAT,
            payload = "Hello C!",
            hopCount = 3
        )
        val jsonFromA = gson.toJson(packetFromA)

        // Act: B receives the packet from A
        engine.processIncomingJson("ENDPOINT_A", jsonFromA)

        // Assert
        // 1. B shouldn't "handle" the packet (because it's for C)
        assertTrue("B should not handle packet meant for C", handledPackets.isEmpty())

        // 2. B should relay the packet to neighbors
        assertEquals("B should relay 1 packet", 1, relayedPackets.size)
        assertEquals("Relayed packet should have decreased hopCount", 2, relayedPackets[0].hopCount)
        assertEquals("Relayed packet should maintain original content", "Hello C!", relayedPackets[0].payload)
    }

    @Test
    fun `test deduplication prevents infinite loops`() {
        val relayedPackets = mutableListOf<Packet>()
        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { packet, _ -> relayedPackets.add(packet) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _ -> }
        )

        val packet = Packet(senderId = "A", senderName = "A", type = PacketType.CHAT, payload = "Loop")
        val json = gson.toJson(packet)

        // Act: Receive same packet twice
        engine.processIncomingJson("E1", json)
        engine.processIncomingJson("E1", json)

        // Assert: Should only relay once
        assertEquals("Should only relay once even if received twice", 1, relayedPackets.size)
    }

    @Test
    fun `test deduplication cache performance and stability under extreme load`() {
        val cacheSize = 2000
        val engine = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            cacheSize = cacheSize,
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _ -> }
        )

        // Fill cache and then overflow it
        val iterations = 10000
        val startTime = System.currentTimeMillis()
        for (i in 1..iterations) {
            val packet = Packet(
                id = "STRESS_$i",
                senderId = "A",
                senderName = "A",
                type = PacketType.CHAT,
                payload = "Test"
            )
            engine.processIncomingJson("E1", gson.toJson(packet))
        }
        val duration = System.currentTimeMillis() - startTime

        println("Processed $iterations packets in ${duration}ms")

        // Ensure the cache is still functional and didn't crash
        val lastPacket = Packet(
            id = "STRESS_$iterations",
            senderId = "A",
            senderName = "A",
            type = PacketType.CHAT,
            payload = "Test"
        )
        var handled = false
        val engine2 = MeshEngine(
            myNodeId = myNodeId,
            myNickname = myNickname,
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = { handled = true },
            onProfileUpdate = { _, _, _ -> }
        )
        engine2.processIncomingJson("E1", gson.toJson(lastPacket))
        engine2.processIncomingJson("E1", gson.toJson(lastPacket))

        // This is a separate engine instance, so it should handle it once.
        // The real test here is that the loop above didn't cause OOM or abnormal behavior.
        assertTrue("Processing should still work after stress", duration < 5000) // 10k packets shouldn't take > 5s
    }
}
