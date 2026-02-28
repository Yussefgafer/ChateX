package com.kai.ghostmesh.model

object Constants {
    const val SERVICE_ID = "com.kai.chatex.SERVICE_ID"
    const val PREFS_NAME = "ghost_mesh_prefs"
    const val KEY_NICKNAME = "nickname"
    const val KEY_STATUS = "status"
    const val KEY_NODE_ID = "node_id"
    
    // Mesh Settings
    const val MAX_PROCESSED_PACKETS = 1000
    const val DEFAULT_HOP_COUNT = 3
}

enum class PacketType {
    CHAT,
    IMAGE,
    VOICE,
    FILE,
    PROFILE_SYNC,
    ACK,
    TYPING_START,
    TYPING_STOP,
    REACTION,
    LAST_SEEN,
    PROFILE_IMAGE,
    KEY_EXCHANGE,
    LINK_STATE,
    BATTERY_HEARTBEAT,
    GATEWAY_AVAILABLE,
    TUNNEL
}
