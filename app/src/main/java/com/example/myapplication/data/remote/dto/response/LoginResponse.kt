package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("codeStatus") val codeStatus: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: LoginData,
    @SerializedName("timestamp") val timestamp: String
)