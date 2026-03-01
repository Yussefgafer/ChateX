package com.kai.ghostmesh.mesh

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NostrIntegrationTest {
    private val gson = Gson()

    @Test
    fun `test packet to nostr event mapping`() {
        val packet = Packet(
            senderId = "node-a",
            senderName = "Alice",
            receiverId = "node-b",
            type = PacketType.CHAT,
            payload = "Hello Nostr"
        )

        val createdAt = System.currentTimeMillis() / 1000
        val kind = 4
        val content = gson.toJson(packet)

        val tags = JsonArray().apply {
            add(JsonArray().apply { add("p"); add(packet.receiverId) })
        }

        // Structure check
        assertNotNull(content)
        assertEquals(1, tags.size())
        assertEquals("p", tags.get(0).asJsonArray.get(0).asString)
        assertEquals("node-b", tags.get(0).asJsonArray.get(1).asString)

        // Verify we can parse it back
        val parsedPacket = gson.fromJson(content, Packet::class.java)
        assertEquals("node-a", parsedPacket.senderId)
        assertEquals("Hello Nostr", parsedPacket.payload)
    }
}
