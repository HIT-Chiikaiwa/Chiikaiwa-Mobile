package com.example.myapplication.data.remote.dto.request

data class ScheduleInviteRequest(
    val subject: String? = null,
    val location: String? = null,
    val scheduledAt: String? = null,
    val duration: Int? = 0,
    val note: String? = null
)
