package com.example.myapplication.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    val VN_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    val VN_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    val UTC_TIMEZONE: TimeZone = TimeZone.getTimeZone("UTC")

    private val UTC_PARSE_FORMATS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(VN_ZONE_ID)

    fun formatRelativeTime(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "Vừa xong"
        if (rawDate == "Vừa xong" || rawDate.contains("trước")) return rawDate

        val date = parseUtcDate(rawDate) ?: return try {
            val millis = rawDate.toLong()
            formatRelativeTimeFromMillis(millis)
        } catch (e: Exception) {
            rawDate
        }

        return formatRelativeTimeFromMillis(date.time)
    }

    private fun formatRelativeTimeFromMillis(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - timeMillis

        if (diffMillis < 60_000L) {
            return "Vừa xong"
        }

        val diffMinutes = diffMillis / (60 * 1000L)
        if (diffMinutes < 60) {
            return "${diffMinutes} phút trước"
        }

        val diffHours = diffMillis / (60 * 60 * 1000L)
        if (diffHours < 24) {
            return "${diffHours} giờ trước"
        }

        val diffDays = diffMillis / (24 * 60 * 60 * 1000L)
        if (diffDays < 30) {
            return "${diffDays} ngày trước"
        }

        val diffMonths = diffDays / 30
        if (diffMonths < 12) {
            return "${diffMonths} tháng trước"
        }

        val diffYears = diffDays / 365
        return "${diffYears} năm trước"
    }

    fun formatChatTime(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""

        val date = parseUtcDate(rawDate)
        if (date != null) {
            return formatToVnTime(date, "HH:mm")
        }

        return try {
            val instant = Instant.parse(rawDate)
            timeFormatter.format(instant)
        } catch (e: Exception) {
            rawDate
        }
    }

    fun parseUtcDate(rawDate: String?): Date? {
        if (rawDate.isNullOrBlank()) return null
        
        // 1. Try to parse using java.time.Instant (handles Z and offsets like +07:00, +0700)
        try {
            val normalized = rawDate.replace(" ", "T")
            // Check if there is already a timezone indicator. If not, append 'Z' to treat as UTC.
            val hasOffset = normalized.endsWith("Z") || normalized.matches(Regex(".*[+-]\\d{2}:?\\d{2}$"))
            val targetStr = if (hasOffset) normalized else normalized + "Z"
            val instant = Instant.parse(targetStr)
            return Date.from(instant)
        } catch (_: Exception) {
        }

        // 2. Fallback to SimpleDateFormat
        val cleanStr = if (rawDate.contains(".")) rawDate.substringBefore(".") else rawDate
        for (pattern in UTC_PARSE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = UTC_TIMEZONE
                sdf.isLenient = false
                val result = sdf.parse(cleanStr)
                if (result != null) return result
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun formatToVnTime(date: Date, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = VN_TIMEZONE
        return sdf.format(date)
    }

    fun formatUtcToVn(rawDate: String?, pattern: String): String {
        val date = parseUtcDate(rawDate) ?: return rawDate ?: ""
        return formatToVnTime(date, pattern)
    }

    fun utcToVnDate(rawDate: String?): String {
        return formatUtcToVn(rawDate, "yyyy-MM-dd")
    }

    fun formatToUtc(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = UTC_TIMEZONE
        return sdf.format(date)
    }

    fun formatTimeInText(content: String?): String {
        if (content.isNullOrBlank()) return content ?: ""

        val isoPattern = Regex("""\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2})?(\.\d+)?(Z|[+-]\d{2}:?\d{2})?""")

        return isoPattern.replace(content) { matchResult ->
            val rawUtcTime = matchResult.value
            val parsed = parseUtcDate(rawUtcTime)
            if (parsed != null) {
                formatToVnTime(parsed, "HH:mm, dd/MM/yyyy")
            } else {
                rawUtcTime
            }
        }
    }
}
