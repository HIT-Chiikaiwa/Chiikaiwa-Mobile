package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class BlockedUserDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("phone") val phone: String? = null
)
