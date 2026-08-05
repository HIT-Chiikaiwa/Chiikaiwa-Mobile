package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class RegisterDeviceRequest(
    @SerializedName("fcmToken") val fcmToken: String,
    @SerializedName("deviceType") val deviceType: String = "ANDROID",
    @SerializedName("deviceName") val deviceName: String? = null
)
