package com.example.myapplication.data.remote.dto.response

import com.example.myapplication.data.model.Message

data class SearchConversationResponse(
    val conversationId: Long,
    val conversationName: String,
    val avatar: String?,
    val lastMessage: Message?
)
