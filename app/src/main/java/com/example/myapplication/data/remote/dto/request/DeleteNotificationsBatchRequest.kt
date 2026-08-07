package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class DeleteNotificationsBatchRequest(
    @SerializedName("notificationIds") val notificationIds: List<String>
)
