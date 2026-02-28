package com.kai.ghostmesh.mesh

import android.util.Log
import com.google.gson.Gson
import com.kai.ghostmesh.model.Packet
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach

class CloudTransport : MeshTransport {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var callback: MeshTransport.Callback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var myNodeId: String = ""
    private var relayUrl: String = "wss://relay.ghostmesh.net/ws"

    override fun start(nickname: String, isStealth: Boolean) {
        if (isStealth) return

        scope.launch {
            connect()
        }
    }

    private suspend fun connect() {
        try {
            client.webSocket(urlString = relayUrl) {
                session = this
                Log.d("CloudTransport", "Connected to relay")

                // Register with relay
                send(Frame.Text(gson.toJson(mapOf("type" to "REGISTER", "nodeId" to myNodeId))))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        callback?.onPacketReceived("Cloud:Relay", text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CloudTransport", "Connection error: ${e.message}")
            delay(5000)
            connect()
        }
    }

    override fun stop() {
        scope.cancel()
        client.close()
    }

    override fun sendPacket(packet: Packet, endpointId: String?) {
        scope.launch {
            try {
                session?.send(Frame.Text(gson.toJson(packet)))
            } catch (e: Exception) {
                Log.e("CloudTransport", "Send error: ${e.message}")
            }
        }
    }

    override fun setCallback(callback: MeshTransport.Callback) {
        this.callback = callback
    }

    fun setNodeId(nodeId: String) {
        this.myNodeId = nodeId
    }
}
