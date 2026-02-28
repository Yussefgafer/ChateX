package com.kai.ghostmesh.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
        val chunks: Array<ByteArray>
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
            val chunks = splitFileIntoChunks(file)
            val transfer = FileTransfer(
                fileId = fileId,
                fileName = file.name,
                totalSize = file.length(),
                senderId = recipientId
            )
            activeTransfers[fileId] = transfer

            sendFileMetadata(endpointId, fileId, file.name, file.length(), recipientId, chunks.size)

            Thread {
                chunks.forEachIndexed { index, chunk ->
                    sendChunk(endpointId, fileId, index, chunk, recipientId, chunks.size)
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate file transfer", e)
            onFileError(file.name, recipientId, e.message ?: "Transfer failed")
        }
    }

    private fun splitFileIntoChunks(file: File): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(CHUNK_SIZE)

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break
            chunks.add(buffer.copyOf(bytesRead))
        }

        inputStream.close()
        return chunks
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
                    pending.chunks[chunkData.chunkIndex] = chunkData.data
                    
                    val received = pending.chunks.count { it.isNotEmpty() }
                    val progress = received.toFloat() / pending.totalChunks
                    onFileProgress(pending.fileName, pending.senderId, progress)
                    
                    if (received == pending.totalChunks) {
                        reconstructFile(pending)
                    }
                }
            } catch (e: Exception) {
                try {
                    val metadata = gson.fromJson(json, FileMetadata::class.java)
                    pendingFiles[metadata.fileId] = PendingFileTransfer(
                        fileId = metadata.fileId,
                        fileName = metadata.fileName,
                        totalSize = metadata.fileSize,
                        senderId = metadata.senderId,
                        totalChunks = metadata.totalChunks,
                        chunks = Array(metadata.totalChunks) { ByteArray(0) }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse file data", e)
                }
            }
        }
    }

    private fun reconstructFile(pending: PendingFileTransfer) {
        try {
            val outputFile = File(context.cacheDir, "received_${pending.fileName}")
            val outputStream = FileOutputStream(outputFile)

            pending.chunks.forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    outputStream.write(chunk)
                }
            }

            outputStream.close()
            pendingFiles.remove(pending.fileId)
            activeTransfers.remove(pending.fileId)

            onFileComplete(pending.fileName, pending.senderId, outputFile.absolutePath)
            Log.d(TAG, "File transfer complete: ${pending.fileName}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconstruct file", e)
            onFileError(pending.fileId, pending.senderId, "File reconstruction failed")
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
