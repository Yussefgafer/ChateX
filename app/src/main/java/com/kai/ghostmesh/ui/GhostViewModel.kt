package com.kai.ghostmesh.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.data.local.*
import com.kai.ghostmesh.mesh.MeshManager
import com.kai.ghostmesh.model.*
import com.kai.ghostmesh.security.SecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class GhostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val messageDao = database.messageDao()
    private val profileDao = database.profileDao()

    private val prefs = application.getSharedPreferences("chatex_prefs", Context.MODE_PRIVATE)
    private val myNodeId = prefs.getString("node_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("node_id", it).apply()
    }

    private val _userProfile = MutableStateFlow(UserProfile(id = myNodeId, name = "User_${android.os.Build.MODEL}"))
    val userProfile = _userProfile.asStateFlow()

    private val _onlineGhosts = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val onlineGhosts = _onlineGhosts.asStateFlow()

    val allKnownProfiles = profileDao.getAllProfiles().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeChatGhostId = MutableStateFlow<String?>(null)
    val activeChatGhostId = _activeChatGhostId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChatHistory = _activeChatGhostId.flatMapLatest { ghostId ->
        if (ghostId != null) {
            messageDao.getMessagesForGhost(ghostId).map { entities ->
                entities.map { Message(it.senderName, it.content, it.isMe, it.isImage, it.isSelfDestruct, it.expiryTime, it.timestamp) }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val isDiscoveryEnabled = MutableStateFlow(true)
    val isAdvertisingEnabled = MutableStateFlow(true)
    val isHapticEnabled = MutableStateFlow(true)
    val isEncryptionEnabled = MutableStateFlow(true)
    val selfDestructSeconds = MutableStateFlow(0)

    private val meshManager = MeshManager(
        context = application,
        myNodeId = myNodeId,
        onPacketReceived = { packet ->
            viewModelScope.launch {
                when (packet.type) {
                    PacketType.CHAT, PacketType.IMAGE -> {
                        val decryptedPayload = SecurityManager.decrypt(packet.payload)
                        val expiryTime = if (packet.isSelfDestruct) System.currentTimeMillis() + (packet.expirySeconds * 1000) else 0
                        
                        messageDao.insertMessage(MessageEntity(
                            ghostId = packet.senderId,
                            senderName = packet.senderName,
                            content = decryptedPayload,
                            isMe = false,
                            isImage = packet.type == PacketType.IMAGE,
                            isSelfDestruct = packet.isSelfDestruct,
                            expiryTime = expiryTime,
                            timestamp = packet.timestamp
                        ))
                    }
                    PacketType.PROFILE_SYNC -> {
                        val parts = packet.payload.split("|")
                        if (parts.isNotEmpty()) {
                            profileDao.insertProfile(ProfileEntity(packet.senderId, parts[0], parts.getOrNull(1) ?: ""))
                            updateOnlineGhost(packet.senderId, parts[0], parts.getOrNull(1) ?: "")
                        }
                    }
                    else -> {}
                }
            }
        },
        onConnectionChanged = { ghosts ->
            viewModelScope.launch {
                _onlineGhosts.value = ghosts.mapValues { entry -> 
                    val db = profileDao.getProfileById(entry.key)
                    UserProfile(id = entry.key, name = entry.value, status = db?.status ?: "Online")
                }
                syncProfile()
            }
        }
    )

    init {
        viewModelScope.launch {
            while(true) {
                messageDao.deleteExpiredMessages(System.currentTimeMillis())
                delay(2000)
            }
        }
    }

    private fun updateOnlineGhost(id: String, name: String, status: String) {
        val current = _onlineGhosts.value.toMutableMap()
        current[id] = UserProfile(id = id, name = name, status = status)
        _onlineGhosts.value = current
    }

    fun updateMyProfile(name: String, status: String) {
        _userProfile.value = _userProfile.value.copy(name = name, status = status)
        syncProfile()
    }

    private fun syncProfile() {
        val profile = _userProfile.value
        meshManager.sendPacket(Packet(senderId = myNodeId, senderName = profile.name, type = PacketType.PROFILE_SYNC, payload = "${profile.name}|${profile.status}"))
    }

    fun startMesh() = meshManager.startMesh(_userProfile.value.name)
    fun stopMesh() = meshManager.stop()

    fun sendMessage(content: String) {
        val targetId = _activeChatGhostId.value ?: return
        val destruct = selfDestructSeconds.value > 0
        
        val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(content) else content
        meshManager.sendPacket(Packet(
            senderId = myNodeId,
            senderName = _userProfile.value.name,
            receiverId = targetId,
            type = PacketType.CHAT,
            payload = encryptedPayload,
            isSelfDestruct = destruct,
            expirySeconds = selfDestructSeconds.value
        ))
        
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                ghostId = targetId,
                senderName = "Me",
                content = content,
                isMe = true,
                isImage = false,
                isSelfDestruct = destruct,
                expiryTime = if (destruct) System.currentTimeMillis() + (selfDestructSeconds.value * 1000) else 0,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun sendImage(uri: Uri) {
        val targetId = _activeChatGhostId.value ?: return
        viewModelScope.launch {
            val base64 = uriToBase64(uri) ?: return@launch
            val destruct = selfDestructSeconds.value > 0
            
            val encryptedPayload = if (isEncryptionEnabled.value) SecurityManager.encrypt(base64) else base64
            meshManager.sendPacket(Packet(
                senderId = myNodeId,
                senderName = _userProfile.value.name,
                receiverId = targetId,
                type = PacketType.IMAGE,
                payload = encryptedPayload,
                isSelfDestruct = destruct,
                expirySeconds = selfDestructSeconds.value
            ))

            messageDao.insertMessage(MessageEntity(
                ghostId = targetId,
                senderName = "Me",
                content = base64, // Store base64 locally for now
                isMe = true,
                isImage = true,
                isSelfDestruct = destruct,
                expiryTime = if (destruct) System.currentTimeMillis() + (selfDestructSeconds.value * 1000) else 0,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Compress significantly for Mesh Relay
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    fun clearHistory() = viewModelScope.launch { messageDao.clearAllMessages() }
    fun setActiveChat(ghostId: String?) { _activeChatGhostId.value = ghostId }
}
