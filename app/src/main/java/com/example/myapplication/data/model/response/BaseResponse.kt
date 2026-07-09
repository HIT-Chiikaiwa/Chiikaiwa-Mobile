package com.example.myapplication.data.model.response

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("codeStatus") val codeStatus: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T,
    @SerializedName("timestamp") val timestamp: String
)
