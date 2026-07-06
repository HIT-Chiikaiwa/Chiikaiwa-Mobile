package com.example.myapplication.data.model.request

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String,
    val confirmPassword: String
)
