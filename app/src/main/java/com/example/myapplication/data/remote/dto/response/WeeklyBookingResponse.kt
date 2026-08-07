package com.example.myapplication.data.remote.dto.response

data class WeeklyBookingResponse(
    val weekStart: String? = null,
    val weekEnd: String? = null,
    val daySchedules: Map<String, List<BookingDto>>? = null
)
