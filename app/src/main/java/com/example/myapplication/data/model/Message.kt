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
    val isDeleted: Boolean = false,
    val replyToMessage: ReplyMessage? = null,
    val forwardedFrom: ReplyMessage? = null,
    val reactions: List<Reaction> = emptyList()
)

data class ReplyMessage(
    val id: String?,
    val senderName: String?,
    val content: String?,
    val messageType: String?
)

data class Reaction(
    val emoji: String?,
    val count: Int?,
    val userIds: List<String> = emptyList(),
    val userNames: List<String> = emptyList()
)
