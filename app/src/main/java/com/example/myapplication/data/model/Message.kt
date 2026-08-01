package com.example.myapplication.data.model

data class Message(
    val id: String,
    val conversationId: String,
    val sender: User,
    val content: String,
    val type: MessageType,
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: String = "",
    val updatedAt: String = "",
    val isRecalled: Boolean = false,
    val isDeleted: Boolean = false
)
