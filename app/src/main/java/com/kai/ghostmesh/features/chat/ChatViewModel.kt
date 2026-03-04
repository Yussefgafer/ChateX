package com.kai.ghostmesh.features.chat

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val container = (application as? GhostApplication)?.container 
        ?: (application.applicationContext as? GhostApplication)?.container

    private val repository: GhostRepository? = container?.repository
    private val meshManager: MeshManager? = container?.meshManager
    private val audioManager = AudioManager(application)

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _activeChatGhostId.flatMapLatest { id ->
        if (id == null || repository == null) flowOf(emptyList()) else repository.getMessagesForGhost(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _typingPeers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val typingPeers = _typingPeers.map { it.keys }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    data class ReplyInfo(val messageId: String, val messageContent: String, val senderName: String)
    private val _replyToMessage = MutableStateFlow<ReplyInfo?>(null)
    val replyToMessage = _replyToMessage.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

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
                kotlinx.coroutines.delay(2000)
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
        if (content.isBlank() || container == null || meshManager == null) return
        val targetId = _activeChatGhostId.value ?: "ALL"
        val replyInfo = _replyToMessage.value

        viewModelScope.launch {
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

            val packetId = java.util.UUID.randomUUID().toString()
            val signature = SecurityManager.signPacket(packetId, payloadToSend)

            val packet = Packet(
                id = packetId,
                senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId, type = PacketType.CHAT,
                payload = payloadToSend,
                isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds, hopCount = hopLimit,
                replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName,
                signature = signature,
                isEncrypted = actualEncrypted
            )
            meshManager.sendPacket(packet)
            if (targetId != "ALL") {
                repository?.saveMessage(packet.copy(payload = content, isEncrypted = actualEncrypted), isMe = true, isImage = false, isVoice = false, isVideo = false, expirySeconds = selfDestructSeconds, maxHops = hopLimit, replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName)
            }
        }
        _replyToMessage.value = null
    }

    fun sendTyping(isTyping: Boolean, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        if (targetId == "ALL" || container == null || meshManager == null) return
        viewModelScope.launch {
            val payload = ""
            val packetId = java.util.UUID.randomUUID().toString()
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

    fun sendImage(uri: Uri, isEncryptionEnabled: Boolean, selfDestructSeconds: Int, hopLimit: Int, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        if (container == null || meshManager == null) return
        viewModelScope.launch {
            try {
                ImageUtils.uriToBase64(getApplication(), uri, 2 * 1024 * 1024)?.let { base64 ->
                    var actualEncrypted = false
                    val encryptedPayload = if (isEncryptionEnabled) {
                        val res = SecurityManager.encrypt(base64, targetId)
                        if (res.isSuccess) {
                            actualEncrypted = true
                            res.getOrThrow()
                        } else {
                            _error.emit("Image encryption failed, sending as plain text...")
                            base64
                        }
                    } else base64

                    val packetId = java.util.UUID.randomUUID().toString()
                    val signature = SecurityManager.signPacket(packetId, encryptedPayload)

                    val packet = Packet(
                        id = packetId,
                        senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId,
                        type = PacketType.IMAGE, payload = encryptedPayload,
                        isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds,
                        hopCount = hopLimit, signature = signature,
                        isEncrypted = actualEncrypted
                    )
                    meshManager.sendPacket(packet)
                    repository?.saveMessage(packet.copy(payload = base64, isEncrypted = actualEncrypted), isMe = true, isImage = true, isVoice = false, isVideo = false, expirySeconds = selfDestructSeconds, maxHops = hopLimit)
                }
            } catch (e: Exception) {
                _error.emit(getApplication<Application>().getString(R.string.error_send_image_failed, e.message))
            }
        }
    }

    fun sendVideo(uri: Uri, isEncryptionEnabled: Boolean, selfDestructSeconds: Int, hopLimit: Int, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        if (container == null || meshManager == null) return
        viewModelScope.launch {
            try {
                // Correctly read video file via content resolver
                ImageUtils.uriToBase64(getApplication(), uri, 5 * 1024 * 1024)?.let { base64 ->
                    var actualEncrypted = false
                    val encryptedPayload = if (isEncryptionEnabled) {
                        val res = SecurityManager.encrypt(base64, targetId)
                        if (res.isSuccess) {
                            actualEncrypted = true
                            res.getOrThrow()
                        } else {
                            _error.emit("Video encryption failed, sending as plain text...")
                            base64
                        }
                    } else base64

                    val packetId = java.util.UUID.randomUUID().toString()
                    val signature = SecurityManager.signPacket(packetId, encryptedPayload)

                    val packet = Packet(
                        id = packetId,
                        senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId,
                        type = PacketType.VIDEO, payload = encryptedPayload,
                        isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds,
                        hopCount = hopLimit, signature = signature,
                        isEncrypted = actualEncrypted
                    )
                    meshManager.sendPacket(packet)
                    repository?.saveMessage(packet.copy(payload = base64, isEncrypted = actualEncrypted), isMe = true, isImage = false, isVoice = false, isVideo = true, expirySeconds = selfDestructSeconds, maxHops = hopLimit)
                }
            } catch (e: Exception) {
                _error.emit(getApplication<Application>().getString(R.string.error_send_video_failed, e.message))
            }
        }
    }

    fun deleteMessage(id: String) = viewModelScope.launch { repository?.deleteMessage(id) }
    fun startRecording() = audioManager.startRecording()
    fun stopRecording() = audioManager.stopRecording()
    fun playVoice(base64: String) = audioManager.playAudio(base64)
}
