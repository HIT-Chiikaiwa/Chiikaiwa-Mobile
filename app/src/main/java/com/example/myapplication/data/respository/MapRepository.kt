package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.response.BaseResponse
import com.example.myapplication.data.model.response.NearbyUserResponse
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.utils.Resource

class MapRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getNearbyUsers(lat: Double, lng: Double, radius: Double): Resource<BaseResponse<List<NearbyUserResponse>>> {
        return safeApiCall { api.getNearbyUsers(lat, lng, radius) }
    }
}