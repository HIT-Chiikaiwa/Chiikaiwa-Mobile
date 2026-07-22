package com.example.myapplication.data.model

data class Attachment(
    val id: Long,
    val url: String,
    val thumbnail: String?,
    val fileName: String?,
    val fileSize: Long?,
    val mimeType: String?
)
