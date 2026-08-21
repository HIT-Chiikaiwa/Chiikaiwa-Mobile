package com.example.myapplication.data.repository.map
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.NearbyUserResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import com.example.myapplication.data.remote.api.ApiService

class MapRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun updateLocation(lat: Double, lng: Double): Resource<BaseResponse<com.example.myapplication.data.remote.dto.response.CommonResponse>> {
        return safeApiCall { api.updateLocation(com.example.myapplication.data.remote.dto.request.UpdateLocationRequest(lat, lng)) }
    }

    suspend fun removeLocation(): Resource<BaseResponse<com.example.myapplication.data.remote.dto.response.CommonResponse>> {
        return safeApiCall { api.removeLocation() }
    }

    suspend fun getNearbyUsers(lat: Double, lng: Double, radius: Double): Resource<BaseResponse<List<NearbyUserResponse>>> {
        return safeApiCall { api.getNearbyUsers(lat, lng, radius) }
    }
}
