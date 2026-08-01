package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class ActionStatusDto(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String? = null
)
