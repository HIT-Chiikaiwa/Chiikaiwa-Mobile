package com.example.myapplication.data.model

data class Conversation(
    val id: Long,
    val type: ConversationType,
    val name: String,
    val avatar: String?,
    val ownerId: Long?,
    val lastMessage: Message?,
    val lastMessageTime: String,
    val lastSenderId: Long?,
    val memberCount: Int,
    val unreadCount: Int,
    val createdAt: String,
    val updatedAt: String
)
