package com.kai.ghostmesh.mesh

import com.kai.ghostmesh.core.mesh.MeshEngine
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityAuditTest {

    private lateinit var engine: MeshEngine
    private val receivedPackets = java.util.Collections.synchronizedList(mutableListOf<Packet>())

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(SecurityManager)

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
    fun testTamperedPayloadRejection() = runTest {
        val packetId = "p1"
        val senderId = "peer1"
        val payload = "Clean"
        val tamperedPayload = "Tampered"
        val sig = "validsig"
        val now = System.currentTimeMillis()

        every { SecurityManager.verifyPacket(senderId, packetId, any(), sig) } answers {
            val p = it.invocation.args[2] as String
            p == payload
        }

        val tamperedJson = """{"id":"$packetId","senderId":"$senderId","senderName":"Peer 1","receiverId":"me","type":"CHAT","payload":"$tamperedPayload","signature":"$sig","protocolVersion":1,"timestamp":$now}"""

        engine.processIncomingJson("endpoint1", tamperedJson)
        delay(500)

        assertTrue("Tampered packet should be rejected", receivedPackets.isEmpty())
    }

    @Test
    fun testExpiredTimestampRejection() = runTest {
        val packetId = "p1"
        val senderId = "peer1"
        val oldTimestamp = System.currentTimeMillis() - 7200000

        val expiredJson = """{"id":"$packetId","senderId":"$senderId","senderName":"Peer 1","receiverId":"me","type":"CHAT","payload":"Expired","timestamp":$oldTimestamp,"signature":"sig","protocolVersion":1}"""

        engine.processIncomingJson("endpoint1", expiredJson)
        delay(500)

        assertTrue("Expired packet should be rejected", receivedPackets.isEmpty())
    }
}
