package com.kai.ghostmesh.mesh

import org.junit.Ignore

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PathResilienceTest {

    private lateinit var engine: MeshEngine
    private val neighborPackets = mutableListOf<Pair<Packet, String?>>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(SecurityManager)
        every { SecurityManager.verifyPacket(any(), any(), any(), any()) } returns true
        every { SecurityManager.signPacket(any(), any()) } returns "sig"

        neighborPackets.clear()
        engine = MeshEngine(
            myNodeId = "me",
            myNickname = "MainNode",
            onSendToNeighbors = { packet, exceptId -> neighborPackets.add(packet to exceptId) },
            onHandlePacket = { },
            onProfileUpdate = { _, _, _, _, _ -> }
        )
    }

    @Test
    fun testMultiPathFallback() = runTest {
        val destId = "target_peer"

        val primaryHeartbeat = """{"id":"hb1","senderId":"$destId","senderName":"Target","receiverId":"ALL","type":"BATTERY_HEARTBEAT","payload":"HB","senderBattery":100,"signature":"sig","protocolVersion":1,"pathCost":0}"""
        engine.processIncomingJson(primaryHeartbeat, "primary_endpoint")

        val packet = Packet(id = "p1", senderId = "me", senderName = "Me", receiverId = destId, type = PacketType.CHAT, payload = "Hello", signature = "sig")
        engine.sendPacket(packet)

        advanceUntilIdle()

        assertTrue(neighborPackets.isNotEmpty())
    }
}
