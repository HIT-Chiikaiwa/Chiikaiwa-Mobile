package com.example.myapplication.data.model

data class Message(
    val id: String,
    val conversationId: String,
    val sender: User,
    val content: String,
    val type: MessageType,
    val status: MessageStatus = MessageStatus.SENT,
    val replyMessage: Message? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val isPinned: Boolean = false,
    val isRecalled: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val isDeleted: Boolean = false
)
