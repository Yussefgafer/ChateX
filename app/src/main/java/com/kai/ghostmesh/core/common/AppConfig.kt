package com.kai.ghostmesh.core.common

/**
 * Centralized configuration for the ChateX Mesh Engine.
 * All architectural constants must be defined here to avoid hardcoding.
 */
object AppConfig {

    object Network {
        const val DEFAULT_LAN_PORT = 54321
        const val DISCOVERY_TIMEOUT_MS = 15000L
        const val KEEPALIVE_INTERVAL_MS = 30000L
        const val MAX_PACKET_SIZE_BYTES = 1024 * 1024 // 1MB for mesh packets
        const val CHUNK_SIZE_BYTES = 16384 // 16KB for file transfers
        const val RECONNECT_DELAY_MS = 5000L
    }

    object Security {
        const val KEY_EXCHANGE_TIMEOUT_MS = 10000L
        const val SESSION_VALIDITY_MS = 3600000L // 1 hour
        const val PBKDF2_ITERATIONS = 2048
        const val AES_GCM_TAG_LENGTH = 128
    }

    object Storage {
        const val DATABASE_NAME = "chatex_mesh.db"
        const val MESSAGE_HISTORY_LIMIT = 1000
        const val CACHE_PRUNE_INTERVAL_MS = 600000L // 10 minutes
    }

    object Performance {
        const val MESH_DISPATCHER_THREADS = 4
        const val MAX_CONCURRENT_TRANSFERS = 3
    }
}
