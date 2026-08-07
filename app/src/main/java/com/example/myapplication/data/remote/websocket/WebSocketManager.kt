package com.example.myapplication.data.remote.websocket

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object WebSocketManager : SocketListener {

    private val stompManager = StompManager()
    private val _messageFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val messageFlow: SharedFlow<Pair<String, String>> = _messageFlow

    private var activeUserId: String = ""
    private var activeConversationId: String = ""

    init {
        stompManager.listener = this
    }

    fun connect(url: String, token: String) {
        stompManager.connect(url, token)
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

    fun sendMessage(conversationId: String, content: String, type: String = "TEXT", senderId: String = activeUserId) {
        val jsonPayload = """{"conversationId":"$conversationId","senderId":"$senderId","content":"$content","type":"$type","messageType":"$type"}"""
        stompManager.send("/app/chat.send", jsonPayload)
    }

    fun sendReadReceipt(conversationId: String) {
        val jsonPayload = """{"conversationId":"$conversationId"}"""
        stompManager.send("/app/chat.read", jsonPayload)
    }

    fun sendTypingSignal(conversationId: String, isTyping: Boolean) {
        val jsonPayload = """{"conversationId":"$conversationId","isTyping":$isTyping}"""
        stompManager.send("/app/chat.typing", jsonPayload)
    }

    override fun onConnected() {
        if (activeUserId.isNotEmpty()) {
            subscribeToChat(activeUserId, activeConversationId)
        }
    }

    override fun onDisconnected() {}

    override fun onMessageReceived(destination: String, body: String) {
        _messageFlow.tryEmit(Pair(destination, body))
    }
}
