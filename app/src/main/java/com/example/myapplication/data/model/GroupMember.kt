package com.example.myapplication.data.model

data class GroupMember(
    val id: Long,
    val conversationId: Long,
    val user: User,
    val role: MemberRole,
    val joinedAt: String
)
