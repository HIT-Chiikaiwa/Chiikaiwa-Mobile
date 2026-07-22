package com.example.myapplication.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class PageResponse<T>(
    @SerializedName("content") val content: List<T> = emptyList(),
    @SerializedName("pageable") val pageable: PageableInfo? = null,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("last") val last: Boolean = true,
    @SerializedName("first") val first: Boolean = true,
    @SerializedName("empty") val empty: Boolean = true
)

data class PageableInfo(
    @SerializedName("pageNumber") val pageNumber: Int = 0,
    @SerializedName("pageSize") val pageSize: Int = 20
)
