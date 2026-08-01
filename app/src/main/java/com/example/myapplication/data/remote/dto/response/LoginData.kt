package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class LoginData(
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("id") val id: String,
    @SerializedName("authorities") val authorities: List<Authority>
)