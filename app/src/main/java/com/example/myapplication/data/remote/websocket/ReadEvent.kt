package com.example.myapplication.data.remote.websocket

data class ReadEvent(
    val conversationId: Long,
    val userId: Long,
    val readTime: String
)
