package com.example.myapplication.data.remote.dto.request

data class VerifyOtpRequest(
    val email: String,
    val otpCode: String
)