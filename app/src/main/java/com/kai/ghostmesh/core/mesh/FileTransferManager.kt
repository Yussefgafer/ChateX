package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.security.SecurityManager
import com.kai.ghostmesh.core.util.GhostLog as Log
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * FileTransferManager: Transport-agnostic file streaming via mesh packets.
 */
class FileTransferManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val sendPacket: (Packet) -> Unit,
    private val onFileProgress: (String, String, Float) -> Unit,
    private val onFileComplete: (String, String, String) -> Unit,
    private val onFileError: (String, String, String) -> Unit
) {
    private val TAG = "FileTransferManager"

    companion object {
        const val CHUNK_SIZE = 16384 // 16KB for low-RAM targets
    }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTransfers = ConcurrentHashMap<String, FileTransfer>()
    private val pendingFiles = ConcurrentHashMap<String, PendingFileTransfer>()
    private val chunkAcks = ConcurrentHashMap<String, MutableSet<Int>>()
    private val packetToChunkMap = ConcurrentHashMap<String, Pair<String, Int>>()
    private val gson = Gson()

    data class FileTransfer(
        val fileId: String,
        val fileName: String,
        val totalSize: Long,
        val senderId: String,
        val isDownload: Boolean,
        var bytesTransferred: Long = 0,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class PendingFileTransfer(
        val fileId: String,
        val fileName: String,
        val totalSize: Long,
        val senderId: String,
        val totalChunks: Int,
        var chunksReceived: Int = 0,
        val tempFile: File,
        val isDownload: Boolean = true
    )

    fun initiateFileTransfer(file: File, recipientId: String) {
        if (!file.exists()) {
            onFileError("Local", recipientId, "File not found")
            return
        }

        val fileId = java.util.UUID.randomUUID().toString()
        
        try {
            val totalSize = file.length()
            val totalChunks = ((totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()

            val transfer = FileTransfer(
                fileId = fileId,
                fileName = file.name,
                totalSize = totalSize,
                senderId = recipientId,
                isDownload = false
            )
            activeTransfers[fileId] = transfer

            sendFileMetadata(fileId, file.name, totalSize, recipientId, totalChunks)

            managerScope.launch {
                try {
                    file.inputStream().use { input ->
                        var chunkIndex = 0
                        val buffer = ByteArray(CHUNK_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            val chunk = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)
                            sendChunk(fileId, chunkIndex, chunk, recipientId, totalChunks)
                            chunkIndex++
                            // Throttling for mesh stability
                            delay(100)
                        }
                    }
                } catch (e: Exception) {
                    onFileError(fileId, recipientId, e.message ?: "Read error")
                }
            }
        } catch (e: Exception) {
            onFileError("Local", recipientId, e.message ?: "Init error")
        }
    }

    private fun sendFileMetadata(fileId: String, name: String, size: Long, recipientId: String, totalChunks: Int) {
        val metadata = FileMetadata(fileId, name, size, myNodeId, totalChunks)
        val payloadJson = gson.toJson(metadata)
        val packetId = java.util.UUID.randomUUID().toString()
        val signature = SecurityManager.signPacket(packetId, payloadJson)

        sendPacket(Packet(
            id = packetId,
            senderId = myNodeId,
            senderName = myNickname,
            receiverId = recipientId,
            type = PacketType.FILE,
            payload = payloadJson,
            signature = signature
        ))
    }

    private fun sendChunk(fileId: String, chunkIndex: Int, chunk: ByteArray, recipientId: String, totalChunks: Int) {
        try {
            val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
            val chunkData = FileChunk(fileId, chunkIndex, base64Data, totalChunks)
            val payloadJson = gson.toJson(chunkData)
            val packetId = java.util.UUID.randomUUID().toString()
            val signature = SecurityManager.signPacket(packetId, payloadJson)

            val packet = Packet(
                id = packetId,
                senderId = myNodeId,
                senderName = myNickname,
                receiverId = recipientId,
                type = PacketType.FILE,
                payload = payloadJson,
                signature = signature
            )
            
            packetToChunkMap[packetId] = Pair(fileId, chunkIndex)
            sendPacket(packet)

            val transfer = activeTransfers[fileId]
            if (transfer != null) {
                transfer.bytesTransferred += chunk.size
                val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize.coerceAtLeast(1)
                onFileProgress(transfer.fileName, recipientId, progress)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk", e)
            onFileError(fileId, recipientId, e.message ?: "Chunk error")
        }
    }

    fun receiveFilePacket(packet: Packet) {
        if (packet.type == PacketType.ACK) {
            packetToChunkMap.remove(packet.payload)?.let { (fileId, index) ->
                chunkAcks[fileId]?.add(index)
            }
            return
        }

        val json = packet.payload

        try {
            val chunkData = gson.fromJson(json, FileChunk::class.java)
            if (chunkData?.fileId != null && chunkData.data != null) {
                pendingFiles[chunkData.fileId]?.let { pending ->
                    val rawData = Base64.decode(chunkData.data, Base64.DEFAULT)
                    java.io.RandomAccessFile(pending.tempFile, "rw").use { raf ->
                        raf.seek(chunkData.chunkIndex.toLong() * CHUNK_SIZE)
                        raf.write(rawData)
                    }
                    
                    pending.chunksReceived++
                    val progress = pending.chunksReceived.toFloat() / pending.totalChunks.coerceAtLeast(1)
                    onFileProgress(pending.fileName, pending.senderId, progress)
                    
                    if (pending.chunksReceived >= pending.totalChunks) {
                        finalizeFile(pending)
                    }
                }
                return
            }
        } catch (e: Exception) {}

        try {
            val metadata = gson.fromJson(json, FileMetadata::class.java)
            if (metadata?.fileId != null && metadata.fileName != null && metadata.senderId != null) {
                val availableSpace = context.cacheDir.usableSpace
                if (availableSpace < metadata.fileSize + (50 * 1024 * 1024)) {
                    onFileError(metadata.fileName, metadata.senderId, "Insufficient disk space")
                    return
                }

                val sanitizedFileName = File(metadata.fileName).name
                val tempFile = File(context.cacheDir, "recv_${metadata.fileId}_$sanitizedFileName")
                pendingFiles[metadata.fileId] = PendingFileTransfer(
                    fileId = metadata.fileId,
                    fileName = metadata.fileName,
                    totalSize = metadata.fileSize,
                    senderId = metadata.senderId,
                    totalChunks = metadata.totalChunks,
                    tempFile = tempFile
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse file data", e)
        }
    }

    private fun finalizeFile(pending: PendingFileTransfer) {
        try {
            val sanitizedFileName = File(pending.fileName).name
            val outputFile = File(context.cacheDir, "received_$sanitizedFileName")

            pending.tempFile.inputStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            pending.tempFile.delete()
            onFileComplete(pending.fileName, pending.senderId, outputFile.absolutePath)
            pendingFiles.remove(pending.fileId)
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing file", e)
            onFileError(pending.fileId, pending.senderId, "File finalization failed")
        }
    }

    fun cancelTransfer(fileId: String) {
        activeTransfers.remove(fileId)
        pendingFiles.remove(fileId)
    }

    fun getActiveTransfers(): List<FileTransfer> {
        val transfers = activeTransfers.values.toMutableList()
        pendingFiles.values.forEach { pending ->
            transfers.add(FileTransfer(
                fileId = pending.fileId,
                fileName = pending.fileName,
                totalSize = pending.totalSize,
                senderId = pending.senderId,
                isDownload = true,
                bytesTransferred = pending.chunksReceived.toLong() * CHUNK_SIZE
            ))
        }
        return transfers
    }

    fun stop() {
        managerScope.cancel()
    }

    data class FileMetadata(
        val fileId: String? = null,
        val fileName: String? = null,
        val fileSize: Long = 0,
        val senderId: String? = null,
        val totalChunks: Int = 0
    )

    data class FileChunk(
        val fileId: String? = null,
        val chunkIndex: Int = 0,
        val data: String? = null,
        val totalChunks: Int = 0
    )
}
