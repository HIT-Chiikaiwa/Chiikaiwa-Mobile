package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.NearbyUserResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import com.example.myapplication.data.remote.api.ApiService

class MapRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getNearbyUsers(lat: Double, lng: Double, radius: Double): Resource<BaseResponse<List<NearbyUserResponse>>> {
        return safeApiCall { api.getNearbyUsers(lat, lng, radius) }
    }
}