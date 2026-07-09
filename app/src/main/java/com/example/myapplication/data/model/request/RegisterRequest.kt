package com.example.myapplication.data.model.request

data class RegisterRequest(
    val email: String,
    val password: String,
    val confirmPassword: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dateOfBirth: String
)