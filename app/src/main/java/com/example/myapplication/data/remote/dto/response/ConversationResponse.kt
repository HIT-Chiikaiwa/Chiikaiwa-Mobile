package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ConversationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("groupAvatar") val groupAvatar: String? = null,
    @SerializedName("memberCount") val memberCount: Int = 0,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("hasLeft") val hasLeft: Boolean = false,
    @SerializedName("lastMessage") val lastMessage: MessageResponse? = null
)
