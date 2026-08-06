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

    /** Timezone Việt Nam (UTC+7) - Hardcode để luôn hiển thị đúng giờ VN */
    val VN_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    val VN_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    /** Timezone UTC - Server luôn trả về UTC */
    val UTC_TIMEZONE: TimeZone = TimeZone.getTimeZone("UTC")

    /** Tất cả các format có thể nhận từ server */
    private val UTC_PARSE_FORMATS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss",    // 2026-08-06T07:00:00
        "yyyy-MM-dd'T'HH:mm",       // 2026-08-06T07:00 (KHÔNG CÓ SECONDS)
        "yyyy-MM-dd HH:mm:ss",      // 2026-08-06 07:00:00
        "yyyy-MM-dd HH:mm"          // 2026-08-06 07:00
    )

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(VN_ZONE_ID)

    /**
     * Tính thời gian tương đối so với thời điểm hiện tại:
     * - Dưới 1 phút: "Vừa xong"
     * - Dưới 60 phút: "X phút trước"
     * - Dưới 24 giờ: "X giờ trước"
     * - Dưới 30 ngày: "X ngày trước"
     * - Dưới 12 tháng: "X tháng trước"
     * - Từ 1 năm trở lên: "X năm trước"
     */
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

        if (diffMillis < 60_000L) { // Dưới 1 phút
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

    /**
     * Format thời gian từ server (UTC) thành giờ HH:mm theo VN.
     * Dùng cho: chat message time, booking time trong chat, friends list last message time.
     * Tất cả thời gian từ server đều là UTC → convert sang VN trước khi hiển thị.
     */
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

    /**
     * Parse chuỗi thời gian UTC từ server thành Date object.
     * Hỗ trợ nhiều format:
     * - "2026-08-06T07:00:00" (có seconds)
     * - "2026-08-06T07:00" (KHÔNG có seconds)
     * - "2026-08-06T07:00:00.123456" (có microseconds - tự cắt)
     * - "2026-08-06 07:00:00" (space format)
     */
    fun parseUtcDate(rawDate: String?): Date? {
        if (rawDate.isNullOrBlank()) return null
        val cleanStr = if (rawDate.contains(".")) rawDate.substringBefore(".") else rawDate

        for (pattern in UTC_PARSE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = UTC_TIMEZONE
                sdf.isLenient = false
                val result = sdf.parse(cleanStr)
                if (result != null) return result
            } catch (_: Exception) {
                // Thử pattern tiếp theo
            }
        }
        return null
    }

    /**
     * Format Date thành chuỗi hiển thị theo giờ Việt Nam.
     * @param pattern ví dụ "HH:mm", "HH:mm, dd/MM/yyyy", "yyyy-MM-dd"
     */
    fun formatToVnTime(date: Date, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = VN_TIMEZONE
        return sdf.format(date)
    }

    /**
     * Convert chuỗi UTC từ server sang chuỗi hiển thị giờ VN.
     * Ví dụ: "2026-08-06T07:00:00" (UTC) → "14:00, 06/08/2026" (VN)
     * Ví dụ: "2026-08-06T06:13" (UTC) → "13:13, 06/08/2026" (VN)
     */
    fun formatUtcToVn(rawDate: String?, pattern: String): String {
        val date = parseUtcDate(rawDate) ?: return rawDate ?: ""
        return formatToVnTime(date, pattern)
    }

    /**
     * Convert chuỗi UTC sang ngày (yyyy-MM-dd) theo giờ VN.
     * Quan trọng: 23:00 UTC ngày 5/8 = 06:00 VN ngày 6/8.
     */
    fun utcToVnDate(rawDate: String?): String {
        return formatUtcToVn(rawDate, "yyyy-MM-dd")
    }

    /**
     * Format giờ local VN thành chuỗi UTC để gửi lên server.
     * Dùng khi tạo booking: user chọn 14:00 VN → gửi "07:00" UTC.
     */
    fun formatToUtc(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = UTC_TIMEZONE
        return sdf.format(date)
    }
}
