package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class ScheduleInviteRequest(
    @SerializedName("subject") val subject: String,
    @SerializedName("location") val location: String? = null,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("duration") val duration: Int = 60,
    @SerializedName("note") val note: String? = null
)
