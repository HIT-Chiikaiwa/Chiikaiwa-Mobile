package com.example.myapplication.data.model

data class GroupMember(
    val id: String,
    val conversationId: String,
    val user: User,
    val role: MemberRole,
    val joinedAt: String
)
