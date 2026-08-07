package com.example.myapplication.data.remote.dto.request

data class RegisterDeviceRequest(
    val fcmToken: String,
    val deviceType: String = "ANDROID",
    val deviceName: String? = null
)
