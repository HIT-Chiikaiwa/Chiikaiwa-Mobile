package com.example.myapplication.data.remote.dto.response

import com.example.myapplication.data.model.Message

data class ConversationResponse(
    val conversationId: Long,
    val conversationName: String,
    val avatar: String?,
    val lastMessage: Message?,
    val lastMessageTime: String,
    val unreadCount: Int,
    val memberCount: Int
)
