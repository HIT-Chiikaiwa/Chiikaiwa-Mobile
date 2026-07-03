package com.example.myapplication.data.model.request

data class VerifyOtpRequest(
    val email: String,
    val otpCode: String
)