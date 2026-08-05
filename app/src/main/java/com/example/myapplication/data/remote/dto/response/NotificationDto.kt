package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("actorId") val actorId: String? = null,
    @SerializedName("actorFirstName") val actorFirstName: String? = null,
    @SerializedName("actorLastName") val actorLastName: String? = null,
    @SerializedName("actorAvatar") val actorAvatar: String? = null,
    @SerializedName("targetId") val targetId: String? = null,
    @SerializedName("targetType") val targetType: String? = null,
    @SerializedName("isRead") val isRead: Boolean? = null,
    @SerializedName("createdDate") val createdDate: String? = null
)
