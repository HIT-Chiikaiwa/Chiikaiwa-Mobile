package com.example.myapplication.data.remote.dto.response

import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.MessageStatus

data class SendMessageResponse(
    val messageId: Long,
    val conversationId: Long,
    val sender: User,
    val content: String,
    val createdAt: String,
    val status: MessageStatus
)
