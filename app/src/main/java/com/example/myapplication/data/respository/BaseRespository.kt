package com.example.myapplication.data.repository

import com.example.myapplication.utils.Resource
import retrofit2.Response

open class BaseRepository {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {

            val response = apiCall()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)

            } else {
                Resource.Error(response.message())

            }

        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể kết nối tới server")

        }
    }
}