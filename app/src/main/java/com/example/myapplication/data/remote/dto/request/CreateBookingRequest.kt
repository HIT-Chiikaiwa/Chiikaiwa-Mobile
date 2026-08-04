package com.example.myapplication.data.remote.dto.request

data class CreateBookingRequest(
    val subject: String? = null,
    val scheduledAt: String? = null,
    val durationMinutes: Int? = 0,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationDistrict: String? = null,
    val locationCity: String? = null,
    val note: String? = null,
    val isRecurring: Boolean? = false,
    val reminderMinutesBefore: Int? = 0
)
