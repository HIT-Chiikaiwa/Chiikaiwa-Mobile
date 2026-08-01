package com.example.myapplication.data.model

data class Conversation(
    val id: String,
    val type: ConversationType,
    val name: String,
    val avatar: String? = null,
    val ownerId: String? = null,
    val lastMessage: Message? = null,
    val lastMessageTime: String = "",
    val lastSenderId: String? = null,
    val memberCount: Int = 0,
    val unreadCount: Int = 0,
    val hasLeft: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)
