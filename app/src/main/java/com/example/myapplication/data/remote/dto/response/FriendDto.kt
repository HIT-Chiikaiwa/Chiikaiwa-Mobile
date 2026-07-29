package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class FriendDto(
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdDate") val createdDate: String? = null
)

data class UserSearchDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("friendshipStatus") val friendshipStatus: String? = null
)

data class FriendActionResponse(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("message") val message: String? = null
)
