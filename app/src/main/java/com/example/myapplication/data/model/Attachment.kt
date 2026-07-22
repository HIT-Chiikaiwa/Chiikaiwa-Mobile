package com.example.myapplication.data.model

data class Attachment(
    val id: String,
    val url: String,
    val thumbnail: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
)
