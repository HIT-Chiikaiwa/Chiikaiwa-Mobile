package com.example.myapplication.data.remote.dto.request

data class CreateGroupRequest(
    val name: String,
    val avatar: String?,
    val memberIds: List<Long>
)
