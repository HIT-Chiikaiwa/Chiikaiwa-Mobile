package com.example.myapplication.data.repository

import com.example.myapplication.utils.ErrorMessageMapper
import com.example.myapplication.utils.resource.Resource
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
                    val dataElement = errorObj.get("data")
                    val dataMsg = if (dataElement != null && dataElement.isJsonObject) {
                        val dataObj = dataElement.asJsonObject
                        val explicitMsg = dataObj.get("message")?.asString ?: dataObj.get("error")?.asString
                        if (explicitMsg != null) {
                            explicitMsg
                        } else {
                            val fieldErrors = dataObj.entrySet().mapNotNull { entry ->
                                val valElement = entry.value
                                if (valElement.isJsonPrimitive) valElement.asString else null
                            }
                            if (fieldErrors.isNotEmpty()) {
                                fieldErrors.joinToString("\n")
                            } else null
                        }
                    } else if (dataElement != null && dataElement.isJsonArray) {
                        val dataArray = dataElement.asJsonArray
                        val errors = dataArray.mapNotNull { el ->
                            if (el.isJsonPrimitive) el.asString
                            else if (el.isJsonObject) el.asJsonObject.get("message")?.asString ?: el.asJsonObject.get("error")?.asString
                            else null
                        }
                        if (errors.isNotEmpty()) {
                            errors.joinToString("\n")
                        } else null
                    } else if (dataElement != null && dataElement.isJsonPrimitive) {
                        dataElement.asString
                    } else null

                    dataMsg
                        ?: errorObj.get("message")?.asString
                        ?: errorObj.get("error")?.asString
                        ?: response.message()
                } catch (e: Exception) {
                    response.message()
                }
                val mappedMsg = ErrorMessageMapper.map(errorMsg, response.code())
                Resource.Error(mappedMsg)
            }

        } catch (e: Exception) {
            val mappedMsg = ErrorMessageMapper.map(e.message)
            Resource.Error(mappedMsg)

        }
    }
}