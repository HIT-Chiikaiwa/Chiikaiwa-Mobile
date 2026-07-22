package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("id") val id: String,
    @SerializedName("conversationId") val conversationId: String? = null,
    @SerializedName("senderId") val senderId: String? = null,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("senderAvatar") val senderAvatar: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("messageType") val messageType: String? = null,
    @SerializedName("isRecalled") val isRecalled: Boolean = false,
    @SerializedName("createdDate") val createdDate: String? = null,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("attachments") val attachments: List<AttachmentDto> = emptyList(),
    @SerializedName("replyToMessage") val replyToMessage: MessageRefDto? = null,
    @SerializedName("forwardedFrom") val forwardedFrom: MessageRefDto? = null,
    @SerializedName("reactions") val reactions: List<ReactionDto> = emptyList()
)
