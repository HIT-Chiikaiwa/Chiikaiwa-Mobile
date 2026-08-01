package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class Authority(
    @SerializedName("authority") val authority: String
)