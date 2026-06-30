package com.example.myapplication.data.model.request

data class LoginRequest(
    val emailOrPhone: String,
    val password: String
)