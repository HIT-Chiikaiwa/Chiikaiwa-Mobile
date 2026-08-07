package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("university") val university: String?,
    @SerializedName("majorName") val majorName: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("dateOfBirth") val dateOfBirth: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("trustScore") val trustScore: Double?,
    @SerializedName("buddyActive") val buddyActive: Boolean?,
    @SerializedName("statusTag") val statusTag: String?,
    @SerializedName("subjects") val subjects: List<SubjectDto>?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?
)