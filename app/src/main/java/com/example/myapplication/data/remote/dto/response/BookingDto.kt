package com.example.myapplication.data.remote.dto.response

data class BookingDto(
    val id: String? = null,
    val status: String? = null,
    val subject: String? = null,
    val scheduledAt: String? = null,
    val durationMinutes: Int? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationDistrict: String? = null,
    val locationCity: String? = null,
    val note: String? = null,
    val isRecurring: Boolean? = null,
    val cancelledBy: String? = null,
    val cancelReason: String? = null,
    val reminderMinutesBefore: Int? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val creatorAvatar: String? = null,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerAvatar: String? = null,
    val participantStatus: String? = null,
    val hasRated: Boolean? = null,
    val myRating: Int? = null,
    val messageId: String? = null,
    val conversationId: String? = null,
    val createdDate: String? = null,
    val lastModifiedDate: String? = null,
    val participants: List<BookingParticipantDto>? = null
)

data class BookingParticipantDto(
    val id: String? = null,
    val userId: String? = null,
    val status: String? = null,
    val reminderMinutesBefore: Int? = null,
    val respondedAt: String? = null
)
