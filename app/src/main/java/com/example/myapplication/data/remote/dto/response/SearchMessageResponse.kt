package com.example.myapplication.data.remote.dto.response

import com.example.myapplication.data.model.User

data class SearchMessageResponse(
    val messageId: Long,
    val conversationId: Long,
    val sender: User,
    val content: String,
    val createdAt: String
)
