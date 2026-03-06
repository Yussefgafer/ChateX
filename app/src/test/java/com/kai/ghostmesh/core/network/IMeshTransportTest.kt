package com.kai.ghostmesh.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class IMeshTransportTest {

    class MockTransport : IMeshTransport {
        override val transportId: String = "MOCK"
        override val state = MutableStateFlow<TransportState>(TransportState.Idle)
        override val incomingPackets = MutableSharedFlow<TransportPacket>()
        override val discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())

        var started = false
        var sentPayload: ByteArray? = null

        override suspend fun start() {
            started = true
            state.value = TransportState.Running
        }

        override suspend fun stop() {
            started = false
            state.value = TransportState.Idle
        }

        override suspend fun send(targetId: String, payload: ByteArray): Result<Unit> {
            sentPayload = payload
            return Result.success(Unit)
        }
    }

    @Test
    fun testMockTransportContract() = runBlocking {
        val transport = MockTransport()
        assertEquals("MOCK", transport.transportId)
        assertEquals(TransportState.Idle, transport.state.value)

        transport.start()
        assertEquals(TransportState.Running, transport.state.value)

        val payload = "Hello".toByteArray()
        transport.send("peer1", payload)
        assertEquals(payload, transport.sentPayload)
    }
}
