package com.example.myapplication.data.remote.dto.response

import com.example.myapplication.data.model.Message

data class MessageResponse(
    val conversationId: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val messages: List<Message>
)
