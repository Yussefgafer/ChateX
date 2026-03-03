package com.kai.ghostmesh.core.model

object AppConfig {
    const val VERSION = "1.0.0-SPECTRAL"

    const val DEFAULT_SCAN_INTERVAL_MS = 10000L
    const val DEFAULT_HOP_LIMIT = 5
    const val DEFAULT_CONNECTION_TIMEOUT_S = 30
    const val DEFAULT_CORNER_RADIUS = 16
    const val DEFAULT_FONT_SCALE = 1.0f

    const val KEY_SCAN_INTERVAL = "net_scan_interval"
    const val KEY_HOP_LIMIT = "net_hop_limit"
    const val KEY_CONN_TIMEOUT = "net_conn_timeout"
    const val KEY_CORNER_RADIUS = "ui_corner_radius"
    const val KEY_FONT_SCALE = "ui_font_scale"

    const val KEY_ENABLE_NEARBY = "net_enable_nearby"
    const val KEY_ENABLE_BLUETOOTH = "net_enable_bluetooth"
    const val KEY_ENABLE_LAN = "net_enable_lan"
    const val KEY_ENABLE_WIFI_DIRECT = "net_enable_wifi_direct"

    // Mesh constants
    const val MESH_PORT_LAN = 0
    const val MESH_PORT_WIFI_DIRECT = 8888
    const val PACKET_CACHE_SIZE = 300
    const val PACKET_CACHE_TIMEOUT_MS = 1800000L // 30 mins
    const val ROUTE_PRUNE_TIMEOUT_MS = 600000L // 10 mins
    const val GATEWAY_PRUNE_TIMEOUT_MS = 300000L // 5 mins
}
