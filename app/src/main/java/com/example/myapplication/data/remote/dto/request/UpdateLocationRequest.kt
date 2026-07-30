package com.example.myapplication.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class UpdateLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)