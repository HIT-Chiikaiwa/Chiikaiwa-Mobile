package com.example.myapplication.utils

import com.google.gson.Gson
import com.google.gson.JsonObject

object BookingJsonParser {

    fun isBookingMessage(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        val trimmed = content.trim()
        return trimmed.startsWith("{") && (
            trimmed.contains("bookingId") ||
            trimmed.contains("scheduledAt") ||
            trimmed.contains("duration") ||
            trimmed.contains("locationName") ||
            trimmed.contains("subject")
        )
    }

    fun formatBookingSummary(content: String?): String {
        if (content.isNullOrBlank()) return "Lịch hẹn"
        return try {
            val json = Gson().fromJson(content, JsonObject::class.java)
            val subject = json.get("subject")?.takeIf { !it.isJsonNull }?.asString
            val duration = json.get("durationMinutes")?.takeIf { !it.isJsonNull }?.asInt
                ?: json.get("duration")?.takeIf { !it.isJsonNull }?.asInt
                ?: 30
            val status = json.get("status")?.takeIf { !it.isJsonNull }?.asString?.uppercase() ?: "PENDING"

            val statusText = when (status) {
                "CANCELLED" -> " (Đã hủy)"
                "REJECTED" -> " (Đã từ chối)"
                "CONFIRMED", "ACCEPTED" -> " (Đã chấp nhận)"
                "COMPLETED" -> " (Đã hoàn thành)"
                else -> ""
            }

            if (statusText.isNotEmpty()) {
                "Lịch hẹn$statusText"
            } else if (!subject.isNullOrEmpty()) {
                "$subject ($duration phút)"
            } else {
                "Lịch hẹn ($duration phút)"
            }
        } catch (e: Exception) {
            "Lịch hẹn"
        }
    }

    fun parseBookingJson(
        content: String,
        isOutgoing: Boolean = true,
        senderName: String = "",
        currentUserId: String = ""
    ): BookingMessageHelper.BookingParsed {
        if (!isBookingMessage(content)) {
            return BookingMessageHelper.BookingParsed(title = content, time = "")
        }
        return try {
            val json = Gson().fromJson(content, JsonObject::class.java)
            val scheduledAt = json.get("scheduledAt")?.asString ?: ""
            val status = json.get("status")?.asString ?: "PENDING"
            val bookingId = json.get("bookingId")?.asString
            val cancelReason = json.get("cancelReason")?.asString
                ?: json.get("note")?.asString

            val formattedTime = if (scheduledAt.isNotEmpty()) {
                TimeUtils.formatChatTime(scheduledAt)
            } else ""

            val creatorId = json.get("creatorId")?.asString ?: ""
            val cancelledBy = json.get("cancelledBy")?.asString

            val isCancelledByMe = if (!cancelledBy.isNullOrEmpty() && currentUserId.isNotEmpty()) {
                cancelledBy == currentUserId
            } else if (!cancelledBy.isNullOrEmpty() && creatorId.isNotEmpty()) {
                cancelledBy == creatorId && isOutgoing
            } else {
                isOutgoing
            }

            val title = BookingTextFormatter.generateBookingTitle(status, isOutgoing, senderName, isCancelledByMe)

            BookingMessageHelper.BookingParsed(
                title = title,
                time = formattedTime,
                reason = cancelReason,
                bookingId = bookingId,
                status = status,
                creatorId = creatorId
            )
        } catch (e: Exception) {
            BookingMessageHelper.BookingParsed(title = content, time = "")
        }
    }
}
