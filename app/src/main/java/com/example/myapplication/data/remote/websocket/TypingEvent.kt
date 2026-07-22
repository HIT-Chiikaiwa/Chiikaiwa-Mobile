package com.example.myapplication.data.remote.websocket

data class TypingEvent(
    val conversationId: Long,
    val userId: Long,
    val typing: Boolean
)
