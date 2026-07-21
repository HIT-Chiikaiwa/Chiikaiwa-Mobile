package com.example.myapplication.data.remote.dto.request

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
)
