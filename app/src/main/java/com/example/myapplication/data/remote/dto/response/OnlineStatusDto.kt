package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class OnlineStatusDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("lastSeen") val lastSeen: String?
)
