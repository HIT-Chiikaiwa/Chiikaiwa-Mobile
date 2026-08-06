package com.example.myapplication.utils

import com.google.gson.Gson
import com.google.gson.JsonObject

object BookingTextFormatter {

    fun generateBookingTitle(
        status: String,
        isOutgoing: Boolean,
        senderName: String,
        isCancelledByMe: Boolean
    ): String {
        return when (status.uppercase()) {
            "PENDING" -> {
                if (isOutgoing) "Đặt lịch hẹn thành công!"
                else "${senderName.ifEmpty { "Đối phương" }} đã đặt lịch hẹn với bạn!"
            }
            "CANCELLED" -> {
                if (isCancelledByMe) {
                    "Bạn đã hủy lịch hẹn."
                } else {
                    val name = if (isOutgoing) "Đối phương" else senderName.ifEmpty { "Đối phương" }
                    "$name đã hủy lịch hẹn."
                }
            }
            "CONFIRMED", "ACCEPTED" -> {
                if (isOutgoing) "Đối phương đã chấp nhận lịch hẹn!"
                else "Bạn đã chấp nhận lịch hẹn!"
            }
            "COMPLETED" -> "Cuộc hẹn đã hoàn thành!"
            "REJECTED" -> {
                if (isCancelledByMe) "Bạn đã từ chối lịch hẹn."
                else if (isOutgoing) "Đối phương đã từ chối lịch hẹn."
                else "Bạn đã từ chối lịch hẹn."
            }
            else -> "Cập nhật lịch hẹn"
        }
    }

    fun formatIfBookingJson(
        content: String,
        isOutgoing: Boolean = false,
        senderName: String = "",
        partnerName: String = ""
    ): String {
        if (BookingJsonParser.isBookingMessage(content)) {
            return try {
                val json = Gson().fromJson(content, JsonObject::class.java)
                val subject = json.get("subject")?.asString ?: "Cuộc hẹn"
                val location = json.get("locationName")?.takeIf { !it.isJsonNull }?.asString
                    ?: json.get("location")?.takeIf { !it.isJsonNull }?.asString
                val scheduledAt = json.get("scheduledAt")?.asString ?: ""
                val duration = json.get("durationMinutes")?.asInt
                    ?: json.get("duration")?.asInt
                    ?: 30
                val status = json.get("status")?.asString ?: "PENDING"
                val note = json.get("note")?.asString

                val formattedTime = if (scheduledAt.isNotEmpty()) {
                    TimeUtils.formatChatTime(scheduledAt)
                } else ""

                val builder = StringBuilder()
                builder.append("Lịch").append(subject).append("\n")
                if (formattedTime.isNotEmpty()) {
                    builder.append("⏰ ").append(formattedTime).append(" (").append(duration).append(" phút)\n")
                }
                if (!location.isNullOrEmpty()) {
                    builder.append("Địa điểm: ").append(location).append("\n")
                }
                if (!note.isNullOrEmpty()) {
                    builder.append("Ghi chú: ").append(note).append("\n")
                }
                builder.append("Trạng thái: ").append(translateStatus(status))

                builder.toString()
            } catch (e: Exception) {
                content
            }
        }

        var text = content
        val actions = listOf("đã hủy lịch hẹn", "đã từ chối lịch hẹn", "đã chấp nhận lịch hẹn", "đã đặt lịch hẹn")
        for (action in actions) {
            if (text.contains(action)) {
                val isPartnerAction = partnerName.isNotEmpty() && text.startsWith(partnerName)
                if (isOutgoing || (!isPartnerAction && partnerName.isNotEmpty())) {
                    text = text.replace(Regex("^.*? $action"), "Bạn $action")
                }
                break
            }
        }

        return text
    }

    fun translateStatus(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "Đang chờ xác nhận"
            "CONFIRMED", "ACCEPTED" -> "Đã chấp nhận"
            "COMPLETED" -> "Đã hoàn thành"
            "CANCELLED" -> "Đã hủy"
            "REJECTED" -> "Đã từ chối"
            else -> status
        }
    }

    fun extractStatusFromSystemMessage(content: String): Pair<String, String?>? {
        val lower = content.lowercase()
        return when {
            lower.contains("hủy lịch hẹn") || lower.contains("hủy cuộc hẹn") -> {
                val reason = if (content.contains("Lý do:", ignoreCase = true)) {
                    val afterReason = content.substringAfter("Lý do:", "").ifEmpty {
                        content.substringAfter("lý do:", "")
                    }
                    afterReason.trim()
                } else null
                Pair("CANCELLED", reason)
            }
            lower.contains("chấp nhận lịch hẹn") || lower.contains("đồng ý cuộc hẹn") || lower.contains("đã xác nhận") -> {
                Pair("CONFIRMED", null)
            }
            lower.contains("từ chối lịch hẹn") || lower.contains("từ chối cuộc hẹn") -> {
                Pair("REJECTED", null)
            }
            lower.contains("hoàn thành") -> {
                Pair("COMPLETED", null)
            }
            else -> null
        }
    }
}
