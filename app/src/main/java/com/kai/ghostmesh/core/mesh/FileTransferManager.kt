package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.os.StatFs
import com.kai.ghostmesh.core.mesh.torrent.TorrentEngine
import com.kai.ghostmesh.core.model.Packet
import java.io.File

class FileTransferManager(
    private val context: Context,
    private val myNodeId: String,
    private val onSendPacket: (Packet) -> Unit
) {
    private val torrentEngine = TorrentEngine(context, myNodeId, onSendPacket)
    private val CACHE_LIMIT = 524288000L // 500MB

    fun canAcceptFile(size: Long): Boolean {
        val stat = StatFs(context.cacheDir.absolutePath)
        val available = stat.availableBlocksLong * stat.blockSizeLong
        if (available < CACHE_LIMIT) autoPurge()
        return available > size + CACHE_LIMIT
    }

    private fun autoPurge() {
        val files = context.cacheDir.listFiles() ?: return
        files.sortBy { it.lastModified() }
        var deletedCount = 0
        for (file in files) {
            if (file.delete()) deletedCount++
            if (deletedCount > 10) break
        }
    }

    fun handlePacket(packet: Packet) { }
    fun shareFile(file: File) { if (canAcceptFile(file.length())) torrentEngine.shareFile(file) }
    fun stop() { torrentEngine.stop() }
}
