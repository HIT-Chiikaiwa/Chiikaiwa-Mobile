package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ReactionDto(
    @SerializedName("emoji") val emoji: String,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("userIds") val userIds: List<String> = emptyList()
)
