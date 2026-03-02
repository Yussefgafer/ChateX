package com.kai.ghostmesh.core.mesh

import android.content.Context
import com.kai.ghostmesh.core.util.GhostLog as Log
import com.google.android.gms.nearby.connection.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class FileTransferManager(
    private val context: Context,
    private val connectionsClient: ConnectionsClient,
    private val myNodeId: String,
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

    fun initiateFileTransfer(endpointId: String, file: File, recipientId: String) {
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

            sendFileMetadata(endpointId, fileId, file.name, totalSize, recipientId, totalChunks)

            Thread {
                try {
                    FileInputStream(file).use { inputStream ->
                        val buffer = ByteArray(CHUNK_SIZE)
                        var index = 0
                        while (activeTransfers.containsKey(fileId)) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead <= 0) break
                            val chunk = buffer.copyOf(bytesRead)
                            sendChunk(endpointId, fileId, index, chunk, recipientId, totalChunks)
                            index++
                            // Throttle a bit to prevent overwhelming the connection
                            Thread.sleep(50)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during streaming file transfer", e)
                    onFileError(file.name, recipientId, e.message ?: "Streaming failed")
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate file transfer", e)
            onFileError(file.name, recipientId, e.message ?: "Transfer failed")
        }
    }



    private fun sendFileMetadata(endpointId: String, fileId: String, fileName: String, fileSize: Long, recipientId: String, totalChunks: Int) {
        val metadata = FileMetadata(fileId, fileName, fileSize, recipientId, totalChunks)
        val payload = Payload.fromBytes(gson.toJson(metadata).toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { e -> Log.e(TAG, "Failed to send file metadata", e) }
    }

    private fun sendChunk(endpointId: String, fileId: String, chunkIndex: Int, chunk: ByteArray, recipientId: String, totalChunks: Int) {
        try {
            val chunkData = FileChunk(fileId, chunkIndex, chunk, totalChunks)
            val payload = Payload.fromBytes(gson.toJson(chunkData).toByteArray(StandardCharsets.UTF_8))
            
            // Using a non-blocking approach by not sleeping.
            // Better to use Flow Control or wait for success listener if needed.
            connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    val transfer = activeTransfers[fileId]
                    if (transfer != null) {
                        transfer.bytesTransferred += chunk.size
                        val progress = transfer.bytesTransferred.toFloat() / transfer.totalSize
                        onFileProgress(transfer.fileName, recipientId, progress)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send chunk $chunkIndex", e)
                    onFileError(fileId, recipientId, "Chunk transfer failed")
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk", e)
            onFileError(fileId, recipientId, e.message ?: "Chunk error")
        }
    }

    fun receiveFileChunk(payload: Payload) {
        payload.asBytes()?.let { bytes ->
            val json = String(bytes, StandardCharsets.UTF_8)
            
            try {
                val chunkData = gson.fromJson(json, FileChunk::class.java)
                pendingFiles[chunkData.fileId]?.let { pending ->
                    java.io.RandomAccessFile(pending.tempFile, "rw").use { raf ->
                        raf.seek(chunkData.chunkIndex.toLong() * CHUNK_SIZE)
                        raf.write(chunkData.data)
                    }
                    
                    pending.chunksReceived++
                    val progress = pending.chunksReceived.toFloat() / pending.totalChunks
                    onFileProgress(pending.fileName, pending.senderId, progress)
                    
                    if (pending.chunksReceived == pending.totalChunks) {
                        finalizeFile(pending)
                    }
                }
            } catch (e: Exception) {
                try {
                    val metadata = gson.fromJson(json, FileMetadata::class.java)
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse file data", e)
                }
            }
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
        val data: ByteArray,
        val totalChunks: Int
    )

    private val gson = com.google.gson.Gson()
}
