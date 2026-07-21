package com.example.myapplication.data.model

data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long
)
