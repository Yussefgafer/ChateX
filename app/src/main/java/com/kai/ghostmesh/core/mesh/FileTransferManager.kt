package com.kai.ghostmesh.core.mesh

import android.content.Context
import android.util.Base64
import com.kai.ghostmesh.core.util.GhostLog as Log
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.security.SecurityManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

class FileTransferManager(
    private val context: Context,
    private val myNodeId: String,
    private val myNickname: String,
    private val sendPacket: (Packet) -> Unit,
    private val onFileProgress: (String, String, Float) -> Unit,
    private val onFileComplete: (String, String, String) -> Unit,
    private val onFileError: (String, String, String) -> Unit
) {
    companion object {
        private const val TAG = "FileTransferManager"
        const val CHUNK_SIZE = 32 * 1024
        const val MAX_FILE_SIZE = 100 * 1024 * 1024
    }

    private val activeTransfers = ConcurrentHashMap<String, FileTransfer>()
    private val pendingFiles = ConcurrentHashMap<String, PendingFileTransfer>()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class FileTransfer(
        val fileId: String,
        val fileName: String,
        val totalSize: Long,
        val senderId: String,
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
        val tempFile: File
    )

    fun initiateFileTransfer(file: File, recipientId: String) {
        if (!file.exists()) {
            onFileError(file.name, recipientId, "File not found")
            return
        }

        if (file.length() > MAX_FILE_SIZE) {
            onFileError(file.name, recipientId, "File too large (max 100MB)")
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
                senderId = recipientId
            )
            activeTransfers[fileId] = transfer

            sendFileMetadata(fileId, file.name, totalSize, recipientId, totalChunks)

            managerScope.launch {
                try {
                    FileInputStream(file).use { inputStream ->
                        val buffer = ByteArray(CHUNK_SIZE)
                        var index = 0
                        while (isActive && activeTransfers.containsKey(fileId)) {
                            val bytesRead = withContext(Dispatchers.IO) { inputStream.read(buffer) }
                            if (bytesRead <= 0) break
                            val chunk = buffer.copyOf(bytesRead)
                            sendChunk(fileId, index, chunk, recipientId, totalChunks)
                            index++
                            delay(50)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during streaming file transfer", e)
                    onFileError(file.name, recipientId, e.message ?: "Streaming failed")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate file transfer", e)
            onFileError(file.name, recipientId, e.message ?: "Transfer failed")
        }
    }

    private fun sendFileMetadata(fileId: String, fileName: String, fileSize: Long, recipientId: String, totalChunks: Int) {
        val metadata = FileMetadata(fileId, fileName, fileSize, myNodeId, totalChunks)
        val payloadJson = gson.toJson(metadata)
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
        sendPacket(packet)
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
            
            sendPacket(packet)

            val transfer = activeTransfers[fileId]
            if (transfer != null) {
                transfer.bytesTransferred += chunk.size
                val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize
                onFileProgress(transfer.fileName, recipientId, progress)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk", e)
            onFileError(fileId, recipientId, e.message ?: "Chunk error")
        }
    }

    fun receiveFilePacket(packet: Packet) {
        val json = packet.payload

        try {
            val chunkData = gson.fromJson(json, FileChunk::class.java)
            if (chunkData.fileId != null && chunkData.data != null) {
                pendingFiles[chunkData.fileId]?.let { pending ->
                    val rawData = Base64.decode(chunkData.data, Base64.DEFAULT)
                    java.io.RandomAccessFile(pending.tempFile, "rw").use { raf ->
                        raf.seek(chunkData.chunkIndex.toLong() * CHUNK_SIZE)
                        raf.write(rawData)
                    }
                    
                    pending.chunksReceived++
                    val progress = pending.chunksReceived.toFloat() / pending.totalChunks
                    onFileProgress(pending.fileName, pending.senderId, progress)
                    
                    if (pending.chunksReceived == pending.totalChunks) {
                        finalizeFile(pending)
                    }
                }
                return
            }
        } catch (e: Exception) {}

        try {
            val metadata = gson.fromJson(json, FileMetadata::class.java)
            if (metadata.fileId != null && metadata.fileName != null) {
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

            if (pending.tempFile.renameTo(outputFile)) {
                pendingFiles.remove(pending.fileId)
                activeTransfers.remove(pending.fileId)
                onFileComplete(pending.fileName, pending.senderId, outputFile.absolutePath)
                Log.d(TAG, "File transfer complete: ${pending.fileName}")
            } else {
                throw IOException("Failed to finalize file (rename failed)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize file", e)
            onFileError(pending.fileId, pending.senderId, "File finalization failed")
        }
    }

    fun cancelTransfer(fileId: String) {
        activeTransfers.remove(fileId)
        pendingFiles.remove(fileId)
    }

    fun getActiveTransfers(): List<FileTransfer> = activeTransfers.values.toList()

    fun stop() {
        managerScope.cancel()
    }

    data class FileMetadata(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val senderId: String,
        val totalChunks: Int
    )

    data class FileChunk(
        val fileId: String,
        val chunkIndex: Int,
        val data: String,
        val totalChunks: Int
    )

    private val gson = com.google.gson.Gson()
}
