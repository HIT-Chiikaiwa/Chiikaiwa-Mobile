package com.example.myapplication.data.model.response

data class NearbyUserResponse(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val avatar: String,
    val university: String,
    val majorName: String,
    val statusTag: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double
)