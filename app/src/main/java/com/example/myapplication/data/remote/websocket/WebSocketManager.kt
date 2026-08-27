package com.example.myapplication.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object WebSocketManager : SocketListener {

    private val stompManager = StompManager()
    private val _messageFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val messageFlow: SharedFlow<Pair<String, String>> = _messageFlow
    private val gson = Gson()

    private var activeUserId: String = ""
    private var activeConversationId: String = ""

    init {
        stompManager.listener = this
    }

    fun connect(url: String, token: String) {
        stompManager.connect(url, token)
    }

    fun connect(
        url: String,
        tokenProvider: () -> String,
        refreshTokenProvider: () -> String,
        tokenSaver: (accessToken: String, refreshToken: String) -> Unit
    ) {
        stompManager.connect(url, tokenProvider, refreshTokenProvider, tokenSaver)
    }

    fun disconnect() {
        stompManager.disconnect()
    }

    fun subscribeToChat(userId: String, conversationId: String = "") {
        this.activeUserId = userId
        this.activeConversationId = conversationId

        if (conversationId.isNotEmpty()) {
            stompManager.subscribe("/topic/conversation.$conversationId")
        }
    }

    fun unsubscribeFromChat(conversationId: String) {
        if (conversationId.isNotEmpty()) {
            stompManager.unsubscribe("/topic/conversation.$conversationId")
        }
    }

    fun sendMessage(conversationId: String, content: String, type: String = "TEXT", senderId: String = activeUserId) {
        val payload = JsonObject().apply {
            addProperty("conversationId", conversationId)
            addProperty("senderId", senderId)
            addProperty("content", content)
            addProperty("type", type)
            addProperty("messageType", type)
        }
        stompManager.send("/app/chat.send", gson.toJson(payload))
    }

    fun sendReadReceipt(conversationId: String) {
        val payload = JsonObject().apply {
            addProperty("conversationId", conversationId)
        }
        stompManager.send("/app/chat.read", gson.toJson(payload))
    }

    fun sendTypingSignal(conversationId: String, isTyping: Boolean) {
        val payload = JsonObject().apply {
            addProperty("conversationId", conversationId)
            addProperty("isTyping", isTyping)
        }
        stompManager.send("/app/chat.typing", gson.toJson(payload))
    }

    override fun onConnected() {
        stompManager.subscribe("/user/queue/notifications")
        stompManager.subscribe("/user/queue/friendship")

        if (activeUserId.isNotEmpty()) {
            subscribeToChat(activeUserId, activeConversationId)
        }
    }

    override fun onDisconnected() {}

    override fun onMessageReceived(destination: String, body: String) {
        _messageFlow.tryEmit(Pair(destination, body))
    }
}