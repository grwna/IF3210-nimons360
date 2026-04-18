package com.example.nimons360_far.backend.network

import com.example.nimons360_far.backend.models.*
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

class MapWebSocketManager(private val token: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    var onMemberUpdate: ((MemberPayload) -> Unit)? = null

    fun connect() {
        val request = Request.Builder()
            .url("wss://mad.labpro.hmif.dev/ws/live")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val response = gson.fromJson(text, MemberPresenceResponse::class.java)
                if (response.type == "member_presence_updated") {
                    onMemberUpdate?.invoke(response.payload)
                }
            }
        })
    }

    fun sendPresence(payload: PresencePayload) {
        val message = PresenceRequest(
            payload = payload,
            timestamp = java.time.Instant.now().toString()
        )
        webSocket?.send(gson.toJson(message))
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
    }
}