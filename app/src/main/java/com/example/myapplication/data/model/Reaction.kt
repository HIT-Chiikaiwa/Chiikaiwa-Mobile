package com.example.myapplication.data.model

data class Reaction(
    val emoji: String,
    val count: Int = 0,
    val userIds: List<String> = emptyList()
)
