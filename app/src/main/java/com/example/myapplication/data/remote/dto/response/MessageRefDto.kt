package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class MessageRefDto(
    @SerializedName("id") val id: String,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("messageType") val messageType: String? = null
)
