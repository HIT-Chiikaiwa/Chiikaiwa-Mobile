package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class UpdateGroupRequest(
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("groupAvatar") val groupAvatar: String? = null
)
