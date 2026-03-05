package com.kai.ghostmesh.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for mesh transport layers.
 * Designed for non-blocking, reactive communication.
 */
interface IMeshTransport {
    /**
     * Unique identifier for the transport (e.g., "LAN", "BLE", "WIFI_DIRECT")
     */
    val transportId: String

    /**
     * Current state of the transport.
     */
    val state: StateFlow<TransportState>

    /**
     * Stream of incoming raw packets with source metadata.
     */
    val incomingPackets: Flow<TransportPacket>

    /**
     * Stream of discovered peers.
     */
    val discoveredPeers: StateFlow<List<DiscoveredPeer>>

    /**
     * Initializes and starts the transport.
     */
    suspend fun start()

    /**
     * Gracefully stops the transport and releases resources.
     */
    suspend fun stop()

    /**
     * Sends a raw payload to a specific destination.
     * @param targetId The transport-specific ID of the recipient.
     * @param payload The raw byte array to transmit.
     * @return Result indicating success or failure.
     */
    suspend fun send(targetId: String, payload: ByteArray): Result<Unit>
}

sealed class TransportState {
    object Idle : TransportState()
    object Starting : TransportState()
    object Running : TransportState()
    data class Error(val message: String, val cause: Throwable? = null) : TransportState()
    object Stopping : TransportState()
}

data class TransportPacket(
    val senderId: String,
    val payload: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiscoveredPeer(
    val id: String,
    val name: String,
    val capabilities: Set<String> = emptySet()
)
