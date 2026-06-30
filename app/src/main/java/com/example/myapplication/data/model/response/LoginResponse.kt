package com.example.myapplication.data.model.response

data class LoginResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val id: String,
    val authorities: List<String>
)