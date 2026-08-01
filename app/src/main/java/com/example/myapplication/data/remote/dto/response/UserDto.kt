package com.example.myapplication.data.remote.dto.response

data class UserDto(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val avatar: String?,
    val university: String?,
    val majorName: String?,
    val gender: String?,
    val dateOfBirth: String?,
    val location: String?,
    val trustScore: Double?,
    val buddyActive: Boolean?,
    val statusTag: String?,
    val subjects: List<SubjectDto>?,
    val email: String?,
    val phone: String?
)