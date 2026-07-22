package com.example.myapplication.data.model

data class Message(
    val id: Long,
    val conversationId: Long,
    val sender: User,
    val content: String,
    val type: MessageType,
    val status: MessageStatus,
    val replyMessage: Message?,
    val createdAt: String,
    val updatedAt: String,
    val isPinned: Boolean,
    val attachments: List<Attachment>,
    val reactions: List<Reaction>,
    val isDeleted: Boolean = false
)
