package com.example.myapplication.data.model.request

data class UpdatePersonalInfoRequest (
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dateOfBirth: String,
    val phone: String,
    val email: String
)