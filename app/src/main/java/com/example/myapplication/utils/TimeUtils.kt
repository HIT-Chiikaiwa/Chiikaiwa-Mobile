package com.example.myapplication.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    fun formatChatTime(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            val instant = try {
                Instant.parse(rawDate)
            } catch (e: Exception) {
                val ldt = LocalDateTime.parse(rawDate)
                ldt.toInstant(ZoneOffset.UTC)
            }
            timeFormatter.format(instant)
        } catch (e: Exception) {
            if (rawDate.contains("T") && rawDate.length >= 16) {
                rawDate.substring(11, 16)
            } else {
                rawDate
            }
        }
    }
}
