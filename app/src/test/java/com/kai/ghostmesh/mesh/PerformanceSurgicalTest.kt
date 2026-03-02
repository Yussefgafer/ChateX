package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformanceSurgicalTest {

    @Before
    fun setup() {
        mockkObject(SecurityManager)
        every { SecurityManager.verifyPacket(any(), any(), any(), any()) } returns true
    }

    @Test
    fun testCircularBufferDeduplication() {
        val received = mutableListOf<Packet>()
        val engine = MeshEngine(
            myNodeId = "me",
            myNickname = "me",
            cacheSize = 10,
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = { received.add(it) },
            onProfileUpdate = { _, _, _, _, _ -> }
        )

        val gson = com.google.gson.Gson()

        // Fill cache
        for (i in 0..9) {
            val p = Packet(
                id = "p$i",
                senderId = "s",
                senderName = "s",
                type = PacketType.CHAT,
                payload = "test",
                signature = "valid"
            )
            engine.processIncomingJson("link", gson.toJson(p))
        }
        assertEquals(10, received.size)

        // Try duplicate
        val dup = Packet(
            id = "p5",
            senderId = "s",
            senderName = "s",
            type = PacketType.CHAT,
            payload = "test",
            signature = "valid"
        )
        engine.processIncomingJson("link", gson.toJson(dup))
        assertEquals(10, received.size)

        // Evict p0 by adding p10
        val p10 = Packet(
            id = "p10",
            senderId = "s",
            senderName = "s",
            type = PacketType.CHAT,
            payload = "test",
            signature = "valid"
        )
        engine.processIncomingJson("link", gson.toJson(p10))
        assertEquals(11, received.size)
    }

    @Test
    fun testPacketValidation() {
        val now = System.currentTimeMillis()
        val validPacket = Packet(senderId = "s", senderName = "s", type = PacketType.CHAT, payload = "test", timestamp = now, hopCount = 3)
        assertTrue("Packet should be valid", validPacket.isValid())

        val futurePacket = validPacket.copy(timestamp = now + 600_000L)
        assertFalse("Future packet should be invalid", futurePacket.isValid())

        val stalePacket = validPacket.copy(timestamp = now - 4_000_000L)
        assertFalse("Stale packet should be invalid", stalePacket.isValid())
    }
}
