package com.kai.ghostmesh.core.mesh.transports

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kai.ghostmesh.core.mesh.MeshTransport
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.security.SecurityManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

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

    private suspend fun connect(url: String) {
        try {
            client.webSocket(urlString = url) {
                sessions[url] = this
                Log.d("CloudTransport", "Connected to Nostr relay: $url")

                val subscriptionId = "chatex-$myNodeId"
                val filter = JsonObject().apply {
                    add("kinds", JsonArray().apply { add(4); add(1) })
                    add("#p", JsonArray().apply { add(SecurityManager.getNostrPublicKey()) })
                }

                val shoutFilter = JsonObject().apply {
                    add("kinds", JsonArray().apply { add(1) })
                    add("#t", JsonArray().apply { add("chatex-shout") })
                }

                val req = JsonArray().apply {
                    add("REQ")
                    add(subscriptionId)
                    add(filter)
                    add(shoutFilter)
                }

                send(Frame.Text(gson.toJson(req)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleNostrMessage(text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CloudTransport", "Connection error with $url: ${e.message}")
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
                callback?.onPacketReceived("Cloud:Nostr", content)
            }
        } catch (e: Exception) {
            Log.e("CloudTransport", "Error handling Nostr message: ${e.message}")
        }
    }

    override fun stop() {
        scope.cancel()
        client.close()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        scope.launch {
            try {
                val nostrEvent = createNostrEvent(packet)
                val msg = JsonArray().apply {
                    add("EVENT")
                    add(nostrEvent)
                }
                val json = gson.toJson(msg)

                sessions.values.forEach { session ->
                    try {
                        session.send(Frame.Text(json))
                    } catch (e: Exception) {
                        Log.e("CloudTransport", "Failed to send to a relay: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("CloudTransport", "Send error: ${e.message}")
            }
        }
    }

    private fun createNostrEvent(packet: Packet): JsonObject {
        val pubKey = SecurityManager.getNostrPublicKey()
        val createdAt = System.currentTimeMillis() / 1000
        val kind = if (packet.receiverId == "ALL") 1 else 4
        val content = gson.toJson(packet)

        val tags = JsonArray()
        if (packet.receiverId != "ALL") {
            tags.add(JsonArray().apply { add("p"); add(packet.receiverId) })
        } else {
            tags.add(JsonArray().apply { add("t"); add("chatex-shout") })
        }

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
        val serialized = JsonArray().apply {
            add(0)
            add(pubKey)
            add(createdAt)
            add(kind)
            add(tags)
            add(content)
        }
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
