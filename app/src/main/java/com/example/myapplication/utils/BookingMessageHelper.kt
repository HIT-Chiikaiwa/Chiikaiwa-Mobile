package com.example.myapplication.utils

object BookingMessageHelper {

    data class BookingParsed(
        val title: String,
        val time: String,
        val reason: String? = null,
        val avatarUrl: String? = null,
        val bookingId: String? = null,
        val status: String? = null,
        val creatorId: String? = null
    )

    fun isBookingMessage(content: String): Boolean = BookingJsonParser.isBookingMessage(content)

    fun parseBookingJson(
        content: String,
        isOutgoing: Boolean = true,
        senderName: String = "",
        currentUserId: String = ""
    ): BookingParsed = BookingJsonParser.parseBookingJson(content, isOutgoing, senderName, currentUserId)

    fun formatIfBookingJson(
        content: String,
        isOutgoing: Boolean = false,
        senderName: String = "",
        partnerName: String = ""
    ): String = BookingTextFormatter.formatIfBookingJson(content, isOutgoing, senderName, partnerName)

    fun extractStatusFromSystemMessage(content: String): Pair<String, String?>? =
        BookingTextFormatter.extractStatusFromSystemMessage(content)
}
