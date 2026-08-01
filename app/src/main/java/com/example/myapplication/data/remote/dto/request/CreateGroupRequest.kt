package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class CreateGroupRequest(
    @SerializedName("groupName") val groupName: String,
    @SerializedName("groupAvatar") val groupAvatar: String? = null,
    @SerializedName("memberIds") val memberIds: List<String>
)
