package com.example.myapplication.data.repository

import com.example.myapplication.utils.Resource
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response

open class BaseRepository {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {

            val response = apiCall()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)

            } else {
                val errorMsg = try {
                    val errorBodyString = response.errorBody()?.string()
                    val errorObj = Gson().fromJson(errorBodyString, JsonObject::class.java)
                    errorObj.get("message")?.asString 
                        ?: errorObj.get("error")?.asString 
                        ?: response.message()
                } catch (e: Exception) {
                    response.message()
                }
                Resource.Error(errorMsg.ifEmpty { "Đã xảy ra lỗi" })
            }

        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể kết nối tới server")

        }
    }
}