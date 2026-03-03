package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkFlakinessTest {

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
            onProfileUpdate = { _, _, _, _, _ -> }
        )
    }

    @Test
    fun simulateGossipSyncWithLoss() = runTest {
        val packetLoss = 0.5
        val totalPackets = 20
        val now = System.currentTimeMillis()

        for (i in 1..totalPackets) {
            if (Random.nextDouble() > packetLoss) {
                val json = """{"id":"p$i","senderId":"peer1","senderName":"P1","receiverId":"me","type":"CHAT","payload":"Msg $i","signature":"sig","protocolVersion":1,"timestamp":$now}"""
                engine.processIncomingJson("endpoint1", json)
            }
        }

        delay(1000)

        assertTrue("Some packets should arrive despite loss", receivedPackets.size > 0)
    }
}
