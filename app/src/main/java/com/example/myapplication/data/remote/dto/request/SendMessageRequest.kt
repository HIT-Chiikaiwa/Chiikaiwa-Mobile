package com.example.myapplication.data.remote.dto.request

import com.example.myapplication.data.model.MessageType

data class SendMessageRequest(
    val conversationId: Long,
    val content: String,
    val type: MessageType
)
