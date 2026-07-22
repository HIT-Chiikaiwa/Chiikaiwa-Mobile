package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class AttachmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("fileUrl") val fileUrl: String,
    @SerializedName("fileName") val fileName: String? = null,
    @SerializedName("fileType") val fileType: String? = null,
    @SerializedName("fileSize") val fileSize: Long? = 0
)
