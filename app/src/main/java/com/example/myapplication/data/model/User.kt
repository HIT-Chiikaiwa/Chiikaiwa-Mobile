package com.example.myapplication.data.model

data class User(
    val id: Long,
    val fullName: String,
    val avatar: String?,
    val email: String,
    val online: Boolean,
    val lastSeen: String?
)
