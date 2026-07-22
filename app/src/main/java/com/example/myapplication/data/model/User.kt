package com.example.myapplication.data.model

data class User(
    val id: String,
    val fullName: String,
    val avatar: String? = null,
    val email: String = "",
    val online: Boolean = false,
    val lastSeen: String? = null
)
