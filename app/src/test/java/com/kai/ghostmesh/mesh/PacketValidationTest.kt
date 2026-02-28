package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.model.Packet
import com.kai.ghostmesh.model.PacketType
import com.kai.ghostmesh.model.isValid
import org.junit.Assert.*
import org.junit.Test

class PacketValidationTest {

    @Test
    fun `packet isValid returns true for non-empty fields`() {
        val packet = Packet(
            senderId = "node1",
            senderName = "Ghost",
            type = PacketType.CHAT,
            payload = "Hello"
        )
        assertTrue(packet.isValid())
    }

    @Test
    fun `packet isValid returns false for empty senderId`() {
        val packet = Packet(
            senderId = "",
            senderName = "Ghost",
            type = PacketType.CHAT,
            payload = "Hello"
        )
        assertFalse(packet.isValid())
    }

    @Test
    fun `packet isValid returns false for empty payload`() {
        val packet = Packet(
            senderId = "node1",
            senderName = "Ghost",
            type = PacketType.CHAT,
            payload = ""
        )
        assertFalse(packet.isValid())
    }
}
