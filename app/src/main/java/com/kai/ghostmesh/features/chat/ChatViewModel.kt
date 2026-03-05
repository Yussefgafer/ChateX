package com.kai.ghostmesh.features.chat

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.R
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import com.kai.ghostmesh.core.util.AudioManager
import com.kai.ghostmesh.core.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as? GhostApplication)?.container
        ?: (application.applicationContext as? GhostApplication)?.container

    private val repository: GhostRepository? = container?.repository
    private val meshManager: MeshManager? = container?.meshManager
    private val audioManager = AudioManager(application)

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val messages: StateFlow<List<Message>> = _activeChatGhostId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository?.getMessagesForGhost(id) ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _typingPeers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val typingPeers = _typingPeers.map { it.keys }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _replyToMessage = MutableStateFlow<ReplyInfo?>(null)
    val replyToMessage = _replyToMessage.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // --- Media Staging ---
    data class StagedMedia(
        val uri: Uri,
        val type: MediaType,
        val name: String = "file"
    )
    enum class MediaType { IMAGE, VIDEO, FILE, VOICE }

    private val _stagedMedia = MutableStateFlow<List<StagedMedia>>(emptyList())
    val stagedMedia = _stagedMedia.asStateFlow()

    fun stageMedia(uri: Uri, type: MediaType, name: String = "file") {
        _stagedMedia.update { it + StagedMedia(uri, type, name) }
    }

    fun unstageMedia(uri: Uri) {
        _stagedMedia.update { list -> list.filter { it.uri != uri } }
    }

    fun clearStaging() {
        _stagedMedia.value = emptyList()
    }

    // --- Voice Recording ---
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()
    private var isRecording = false

    fun startRecording() {
        if (isRecording) return
        isRecording = true
        _recordingDuration.value = 0L
        audioManager.startRecording()
        viewModelScope.launch {
            while (isRecording) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        val file = audioManager.stopRecording()
        file?.let {
            stageMedia(Uri.fromFile(it), MediaType.VOICE, "voice_note.m4a")
        }
    }

    private val _fileStatus = meshManager?.fileTransferStatus?.onEach { status ->
        if (status.error != null) {
            _error.emit(getApplication<Application>().getString(R.string.error_file_transfer, status.fileName, status.error))
        }
    }?.launchIn(viewModelScope)

    init {
        viewModelScope.launch {
            meshManager?.incomingPackets?.collect { packet ->
                if (packet.type == PacketType.TYPING_START) {
                    _typingPeers.update { it + (packet.senderId to System.currentTimeMillis()) }
                } else if (packet.type == PacketType.TYPING_STOP) {
                    _typingPeers.update { it - packet.senderId }
                } else if (packet.type == PacketType.CHAT || packet.type == PacketType.IMAGE || packet.type == PacketType.VOICE || packet.type == PacketType.VIDEO) {
                    repository?.saveMessage(
                        packet = packet,
                        isMe = false,
                        isImage = packet.type == PacketType.IMAGE,
                        isVoice = packet.type == PacketType.VOICE,
                        isVideo = packet.type == PacketType.VIDEO,
                        expirySeconds = packet.expirySeconds,
                        maxHops = packet.hopCount
                    )
                    if (packet.senderId != "ALL" && _activeChatGhostId.value == packet.senderId) {
                        meshManager?.sendReadReceipt(packet.senderId, packet.id, packet.senderName)
                    }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _typingPeers.update { it.filter { (_, time) -> now - time < 5000 } }
                delay(2000)
            }
        }
    }

    fun setActiveChat(id: String?) {
        _activeChatGhostId.value = id
        if (id != null) viewModelScope.launch {
            messages.value.filter { !it.isMe && it.status != MessageStatus.READ }.forEach { msg ->
                meshManager?.sendReadReceipt(id, msg.id, msg.sender)
            }
        }
    }
    fun setReplyTo(messageId: String, content: String, sender: String) { _replyToMessage.value = ReplyInfo(messageId, content, sender) }
    fun clearReply() { _replyToMessage.value = null }

    fun sendMessage(content: String, isEncryptionEnabled: Boolean, selfDestructSeconds: Int, hopLimit: Int, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: "ALL"
        val replyInfo = _replyToMessage.value
        val staged = _stagedMedia.value

        viewModelScope.launch {
            staged.forEach { media ->
                // Commercial Logic: Large media is sent via Torrent/FileTransferManager
                val file = uriToFile(media.uri)
                if (file != null) {
                    val pType = when(media.type) {
                        MediaType.IMAGE -> PacketType.IMAGE
                        MediaType.VIDEO -> PacketType.VIDEO
                        MediaType.VOICE -> PacketType.VOICE
                        MediaType.FILE -> PacketType.FILE
                    }
                    meshManager?.initiateFileTransfer(file, targetId, pType)

                    // Save local representation immediately
                    val packetId = UUID.randomUUID().toString()
                    val packet = Packet(
                        id = packetId, senderId = container?.myNodeId ?: "", senderName = myProfile.name,
                        receiverId = targetId, type = pType, payload = file.absolutePath, // Local path for immediate render
                        isEncrypted = false // Metadata handled separately
                    )
                    repository?.saveMessage(packet, isMe = true, isImage = pType == PacketType.IMAGE, isVoice = pType == PacketType.VOICE, isVideo = pType == PacketType.VIDEO, expirySeconds = selfDestructSeconds, maxHops = hopLimit)
                }
            }
            clearStaging()

            if (content.isNotBlank()) {
                var actualEncrypted = false
                val payloadToSend = if (isEncryptionEnabled) {
                    val encryptionResult = SecurityManager.encrypt(content, if(targetId == "ALL") null else targetId)
                    if (encryptionResult.isSuccess) {
                        actualEncrypted = true
                        encryptionResult.getOrThrow()
                    } else {
                        _error.emit("Encryption failed, sending as plain text...")
                        content
                    }
                } else content

                val packetId = UUID.randomUUID().toString()
                val signature = SecurityManager.signPacket(packetId, payloadToSend)

                val packet = Packet(
                    id = packetId,
                    senderId = container?.myNodeId ?: "", senderName = myProfile.name, receiverId = targetId, type = PacketType.CHAT,
                    payload = payloadToSend,
                    isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds, hopCount = hopLimit,
                    replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName,
                    signature = signature,
                    isEncrypted = actualEncrypted
                )
                meshManager?.sendPacket(packet)
                if (targetId != "ALL") {
                    repository?.saveMessage(packet.copy(payload = content, isEncrypted = actualEncrypted), isMe = true, isImage = false, isVoice = false, isVideo = false, expirySeconds = selfDestructSeconds, maxHops = hopLimit, replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName)
                }
            }
        }
        _replyToMessage.value = null
    }

    private fun uriToFile(uri: Uri): File? {
        if (uri.scheme == "file") return File(uri.path!!)
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(getApplication<Application>().cacheDir, "staging_${System.currentTimeMillis()}")
            file.outputStream().use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    fun sendTyping(isTyping: Boolean, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        if (targetId == "ALL" || container == null || meshManager == null) return
        viewModelScope.launch {
            val payload = ""
            val packetId = UUID.randomUUID().toString()
            val signature = SecurityManager.signPacket(packetId, payload)
            meshManager.sendPacket(Packet(
                id = packetId,
                senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId, 
                type = if (isTyping) PacketType.TYPING_START else PacketType.TYPING_STOP, 
                payload = payload,
                signature = signature
            ))
        }
    }

    fun deleteMessage(id: String) = viewModelScope.launch { repository?.deleteMessage(id) }
    fun playVoice(base64: String) = audioManager.playAudio(base64)
    fun stopPlayback() = audioManager.stopPlayback()

    data class ReplyInfo(val messageId: String, val messageContent: String, val senderName: String)
}
