package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MeshConcurrencyTest {

    private lateinit var engine: MeshEngine
    private val receivedPackets = java.util.Collections.synchronizedList(mutableListOf<Packet>())

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(SecurityManager)
        every { SecurityManager.verifyPacket(any(), any(), any(), any()) } returns true

        receivedPackets.clear()
        engine = MeshEngine(
            myNodeId = "me",
            myNickname = "MainNode",
            onSendToNeighbors = { _, _ -> },
            onHandlePacket = { receivedPackets.add(it) },
            onProfileUpdate = { _, _, _, _, _ -> },
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testHighPressureConcurrency() = runTest {
        val count = 150
        val now = System.currentTimeMillis()

        for (i in 0 until count) {
            val packetId = "packet_$i"
            val json = "{\"id\":\"" + packetId + "\",\"senderId\":\"peer1\",\"senderName\":\"Peer 1\",\"receiverId\":\"me\",\"type\":\"CHAT\",\"payload\":\"Msg " + i + "\",\"signature\":\"sig\",\"protocolVersion\":1,\"timestamp\":" + now + "}"
            engine.processIncomingJson("endpoint1", json)
        }

        assertEquals("Should handle all unique packets despite high pressure", count, receivedPackets.size)
    }

    @Test
    fun testDeduplicationUnderPressure() = runTest {
        val count = 100
        val packetId = "duplicate_id"
        val now = System.currentTimeMillis()
        val json = "{\"id\":\"" + packetId + "\",\"senderId\":\"peer1\",\"senderName\":\"Peer 1\",\"receiverId\":\"me\",\"type\":\"CHAT\",\"payload\":\"Duplicate\",\"signature\":\"sig\",\"protocolVersion\":1,\"timestamp\":" + now + "}"

        for (i in 0 until count) {
            engine.processIncomingJson("endpoint1", json)
        }

        assertEquals("Should only handle duplicate packet once", 1, receivedPackets.size)
    }
}
