package com.example.myapplication.data.remote.websocket

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class ChatSocketService(private val stompManager: StompManager) : SocketListener {

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

    fun subscribeToChat(userId: String, conversationId: String = "") {
        this.activeUserId = userId
        this.activeConversationId = conversationId

        stompManager.subscribe("/user/queue/messages")
        if (userId.isNotEmpty()) {
            stompManager.subscribe("/user/$userId/queue/messages")
            stompManager.subscribe("/queue/messages")
        }
        if (conversationId.isNotEmpty()) {
            stompManager.subscribe("/topic/conversations/$conversationId")
            stompManager.subscribe("/topic/messages/$conversationId")
            stompManager.subscribe("/topic/chat/$conversationId")
        }
    }

    fun sendMessage(senderId: String, receiverId: String, text: String, conversationId: String = "") {
        val jsonPayload = """{"senderId":"$senderId","receiverId":"$receiverId","recipientId":"$receiverId","text":"$text","content":"$text","conversationId":"$conversationId"}"""
        stompManager.send("/app/chat.send", jsonPayload)
    }

    fun sendReadReceipt(conversationId: String, messageId: String = "") {
        val jsonPayload = """{"conversationId":"$conversationId","messageId":"$messageId"}"""
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

    override fun onDisconnected() {
    }

    override fun onMessageReceived(destination: String, body: String) {
        _messageFlow.tryEmit(Pair(destination, body))
    }
}