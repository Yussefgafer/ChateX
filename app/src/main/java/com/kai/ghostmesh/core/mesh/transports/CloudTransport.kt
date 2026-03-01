package com.kai.ghostmesh.core.mesh.transports

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.security.SecurityManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class CloudTransport(override val name: String = "Cloud") : MeshTransport {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }

    private val sessions = ConcurrentHashMap<String, DefaultClientWebSocketSession>()
    private var callback: MeshTransport.Callback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var myNodeId: String = ""
    private val proxiedNodeIds = CopyOnWriteArrayList<String>()

    private val relays = listOf(
        "wss://relay.damus.io",
        "wss://nos.lol",
        "wss://relay.snort.social"
    )

    override fun start(nickname: String, isStealth: Boolean) {
        if (isStealth) return
        relays.forEach { url ->
            scope.launch {
                connect(url)
            }
        }
    }

    fun updateProxiedNodes(nodeIds: List<String>) {
        val changed = synchronized(proxiedNodeIds) {
            if (proxiedNodeIds.toSet() != nodeIds.toSet()) {
                proxiedNodeIds.clear()
                proxiedNodeIds.addAll(nodeIds)
                true
            } else false
        }

        if (changed) {
            reSubscribe()
        }
    }

    private fun reSubscribe() {
        scope.launch {
            sessions.forEach { (url, session) ->
                try {
                    val req = createSubscriptionRequest()
                    session.send(Frame.Text(gson.toJson(req)))
                } catch (e: Exception) {}
            }
        }
    }

    private fun createSubscriptionRequest(): JsonArray {
        val subscriptionId = "chatex-$myNodeId"
        val pTags = JsonArray().apply {
            add(SecurityManager.getNostrPublicKey())
            proxiedNodeIds.forEach { add(it) }
        }
        val directFilter = JsonObject().apply {
            add("kinds", JsonArray().apply { add(4); add(1); add(10002) })
            add("#p", pTags)
        }
        val shoutFilter = JsonObject().apply {
            add("kinds", JsonArray().apply { add(1); add(10002) })
            add("#t", JsonArray().apply { add("chatex-shout") })
        }
        return JsonArray().apply {
            add("REQ")
            add(subscriptionId)
            add(directFilter)
            add(shoutFilter)
        }
    }

    private suspend fun connect(url: String) {
        try {
            client.webSocket(urlString = url) {
                sessions[url] = this
                val req = createSubscriptionRequest()
                send(Frame.Text(gson.toJson(req)))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleNostrMessage(text)
                    }
                }
            }
        } catch (e: Exception) {
            sessions.remove(url)
            delay(10000)
            connect(url)
        }
    }

    private fun handleNostrMessage(json: String) {
        try {
            val msg = gson.fromJson(json, JsonArray::class.java)
            if (msg.get(0).asString == "EVENT") {
                val event = msg.get(2).asJsonObject
                val content = event.get("content").asString

                // Nostr stays as JSON for public relay compatibility
                callback?.onPacketReceived("Cloud:Nostr", content)

                // Also attempt binary if it looks like Base64 (not common for direct Kind 4)
                // In actual deployment, the bridge handles this.
            }
        } catch (e: Exception) {}
    }

    override fun stop() {
        scope.cancel()
        client.close()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        scope.launch {
            try {
                val nostrEvent = createNostrEvent(packet)
                val msg = JsonArray().apply { add("EVENT"); add(nostrEvent) }
                val json = gson.toJson(msg)
                sessions.values.forEach { session ->
                    try { session.send(Frame.Text(json)) } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
        }
    }

    private fun createNostrEvent(packet: Packet): JsonObject {
        val pubKey = SecurityManager.getNostrPublicKey()
        val createdAt = System.currentTimeMillis() / 1000
        val kind = when(packet.type) {
            PacketType.BITFIELD -> 10002
            else -> if (packet.receiverId == "ALL") 1 else 4
        }
        val content = gson.toJson(packet)
        val tags = JsonArray()
        if (packet.receiverId != "ALL") tags.add(JsonArray().apply { add("p"); add(packet.receiverId) })
        else tags.add(JsonArray().apply { add("t"); add("chatex-shout") })

        val eventTemplate = JsonObject().apply {
            addProperty("pubkey", pubKey)
            addProperty("created_at", createdAt)
            addProperty("kind", kind)
            add("tags", tags)
            addProperty("content", content)
        }
        val id = calculateEventId(pubKey, createdAt, kind, tags, content)
        eventTemplate.addProperty("id", id)
        val sig = SecurityManager.signNostrEvent(fr.acinq.secp256k1.Hex.decode(id))
        eventTemplate.addProperty("sig", sig)
        return eventTemplate
    }

    private fun calculateEventId(pubKey: String, createdAt: Long, kind: Int, tags: JsonArray, content: String): String {
        val serialized = JsonArray().apply { add(0); add(pubKey); add(createdAt); add(kind); add(tags); add(content) }
        val bytes = gson.toJson(serialized).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        return fr.acinq.secp256k1.Hex.encode(md.digest(bytes))
    }

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    fun setNodeId(nodeId: String) {
        this.myNodeId = nodeId
    }
}
