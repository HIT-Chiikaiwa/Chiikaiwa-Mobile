package com.example.myapplication.data.remote.websocket

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class ChatSocketService(private val stompManager: StompManager) : SocketListener {

    private val _messageFlow = MutableSharedFlow<Pair<String, String>>()
    val messageFlow: SharedFlow<Pair<String, String>> = _messageFlow

    init {
        stompManager.listener = this
    }

    fun connect(url: String, token: String) {
        stompManager.connect(url, token)
    }

    fun subscribeToChat(userId: String) {
        stompManager.subscribe("/user/$userId/queue/messages")
    }

    fun sendPrivateMessage(senderId: String, receiverId: String, text: String) {
        val jsonPayload = """{"senderId":"$senderId","receiverId":"$receiverId","text":"$text"}"""
        stompManager.send("/app/chat.sendPrivate", jsonPayload)
    }

    override fun onConnected() {
    }

    override fun onDisconnected() {
    }

    override fun onMessageReceived(destination: String, body: String) {
        _messageFlow.tryEmit(Pair(destination, body))
    }
}