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
    @SerializedName("isPinned") val isPinned: Boolean? = false,
    @SerializedName("attachments") val attachments: List<AttachmentResponse>? = null,
    @SerializedName("replyToMessage") val replyToMessage: ReplyMessageResponse? = null,
    @SerializedName("forwardedFrom") val forwardedFrom: ReplyMessageResponse? = null,
    @SerializedName("reactions") val reactions: List<ReactionResponse>? = null
)

data class AttachmentResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fileUrl") val fileUrl: String? = null,
    @SerializedName("fileName") val fileName: String? = null,
    @SerializedName("fileType") val fileType: String? = null,
    @SerializedName("fileSize") val fileSize: Long? = 0
)

data class ReplyMessageResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("messageType") val messageType: String? = null
)

data class ReactionResponse(
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("count") val count: Int? = 0,
    @SerializedName("userIds") val userIds: List<String>? = null,
    @SerializedName("userNames") val userNames: List<String>? = null
)
