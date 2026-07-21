package com.example.myapplication.data.remote.dto.request

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String,
    val confirmPassword: String
)
