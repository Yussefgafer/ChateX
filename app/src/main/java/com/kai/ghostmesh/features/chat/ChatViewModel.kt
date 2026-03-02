package com.kai.ghostmesh.features.chat

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.*
import com.kai.ghostmesh.core.security.SecurityManager
import com.kai.ghostmesh.core.util.AudioManager
import com.kai.ghostmesh.core.util.ImageUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GhostApplication).container
    private val repository = container.repository
    private val meshManager = container.meshManager
    private val audioManager = AudioManager(application)

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _activeChatGhostId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMessagesForGhost(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _typingGhosts = MutableStateFlow<Set<String>>(emptySet())
    val typingGhosts = _typingGhosts.asStateFlow()

    data class ReplyInfo(val messageId: String, val messageContent: String, val senderName: String)
    private val _replyToMessage = MutableStateFlow<ReplyInfo?>(null)
    val replyToMessage = _replyToMessage.asStateFlow()

    init {
        viewModelScope.launch {
            meshManager.incomingPackets.collect { packet ->
                if (packet.type == PacketType.TYPING_START) _typingGhosts.value += packet.senderId
                else if (packet.type == PacketType.TYPING_STOP) _typingGhosts.value -= packet.senderId
            }
        }
    }

    fun setActiveChat(id: String?) { _activeChatGhostId.value = id }
    fun setReplyTo(messageId: String, content: String, sender: String) { _replyToMessage.value = ReplyInfo(messageId, content, sender) }
    fun clearReply() { _replyToMessage.value = null }

    fun sendMessage(content: String, isEncryptionEnabled: Boolean, selfDestructSeconds: Int, hopLimit: Int, myProfile: UserProfile) {
        if (content.isBlank()) return
        val targetId = _activeChatGhostId.value ?: "ALL"
        val replyInfo = _replyToMessage.value
        val encryptedPayload = if (isEncryptionEnabled) {
            SecurityManager.encrypt(content, if(targetId == "ALL") null else targetId).getOrElse {
                // If encryption fails, do not send plain text
                return
            }
        } else content

        val packetId = java.util.UUID.randomUUID().toString()
        val signature = SecurityManager.signPacket(packetId, encryptedPayload)

        val packet = Packet(
            id = packetId,
            senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId, type = PacketType.CHAT,
            payload = encryptedPayload,
            isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds, hopCount = hopLimit,
            replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName,
            signature = signature
        )
        meshManager.sendPacket(packet)
        if (targetId != "ALL") viewModelScope.launch {
            repository.saveMessage(packet.copy(payload = content), isMe = true, isImage = false, isVoice = false, expirySeconds = selfDestructSeconds, maxHops = hopLimit, replyToId = replyInfo?.messageId, replyToContent = replyInfo?.messageContent, replyToSender = replyInfo?.senderName)
        }
        _replyToMessage.value = null
    }

    fun sendTyping(isTyping: Boolean, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        if (targetId == "ALL") return
        viewModelScope.launch {
            meshManager.sendPacket(Packet(senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId, type = if (isTyping) PacketType.TYPING_START else PacketType.TYPING_STOP, payload = ""))
        }
    }

    fun sendImage(uri: Uri, isEncryptionEnabled: Boolean, selfDestructSeconds: Int, hopLimit: Int, myProfile: UserProfile) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            ImageUtils.uriToBase64(getApplication(), uri, 2 * 1024 * 1024)?.let { base64 ->
                val encryptedPayload = if (isEncryptionEnabled) {
                    SecurityManager.encrypt(base64, targetId).getOrElse { return@let }
                } else base64

                val packetId = java.util.UUID.randomUUID().toString()
                val signature = SecurityManager.signPacket(packetId, encryptedPayload)

                val packet = Packet(
                    id = packetId,
                    senderId = container.myNodeId, senderName = myProfile.name, receiverId = targetId,
                    type = PacketType.IMAGE, payload = encryptedPayload,
                    isSelfDestruct = selfDestructSeconds > 0, expirySeconds = selfDestructSeconds,
                    hopCount = hopLimit, signature = signature
                )
                meshManager.sendPacket(packet)
                repository.saveMessage(packet.copy(payload = base64), isMe = true, isImage = true, isVoice = false, expirySeconds = selfDestructSeconds, maxHops = hopLimit)
            }
        }
    }

    fun deleteMessage(id: String) = viewModelScope.launch { repository.deleteMessage(id) }
    fun startRecording() = audioManager.startRecording()
    fun stopRecording() = audioManager.stopRecording()
    fun playVoice(base64: String) = audioManager.playAudio(base64)
}
