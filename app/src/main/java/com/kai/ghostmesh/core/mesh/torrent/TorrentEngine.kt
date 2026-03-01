package com.kai.ghostmesh.core.mesh.torrent

import android.content.Context
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class TorrentEngine(
    private val context: Context,
    private val myNodeId: String,
    private val onSendPacket: (Packet) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun shareFile(file: File): String {
        val infoHash = UUID.randomUUID().toString().take(16)
        onSendPacket(Packet(senderId = myNodeId, senderName = "Ghost Seeder", type = PacketType.BITFIELD, payload = "SEED|$infoHash|${file.name}", hopCount = 3))
        return infoHash
    }
    fun stop() { scope.cancel() }
}
