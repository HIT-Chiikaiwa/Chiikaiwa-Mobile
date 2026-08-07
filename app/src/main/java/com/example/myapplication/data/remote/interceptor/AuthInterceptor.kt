package com.example.myapplication.data.remote.interceptor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.network.NetworkConstants
import com.example.myapplication.ui.auth.LoginActivity
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AuthInterceptor(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath
        val isAuthEndpoint = path.contains("/auth/")

        val token = preferenceManager.getAccessToken()
        val requestBuilder = originalRequest.newBuilder()

        if (!token.isNullOrEmpty() && !isAuthEndpoint) {
            requestBuilder.addHeader(
                "Authorization",
                "Bearer $token"
            )
        }

        var response = chain.proceed(requestBuilder.build())

        if (response.code == 401 && !isAuthEndpoint) {
            synchronized(this) {
                val currentToken = preferenceManager.getAccessToken()
                if (!currentToken.isNullOrEmpty() && currentToken != token) {
                    response.close()
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    return chain.proceed(newRequest)
                }

                val refreshToken = preferenceManager.getRefreshToken()
                if (!refreshToken.isNullOrEmpty()) {
                    val refreshed = performTokenRefresh(refreshToken)
                    if (refreshed) {
                        val newToken = preferenceManager.getAccessToken()
                        if (!newToken.isNullOrEmpty()) {
                            response.close()
                            val newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                            return chain.proceed(newRequest)
                        }
                    }
                }

                preferenceManager.logout()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Phiên đăng nhập đã hết hạn.", Toast.LENGTH_LONG).show()
                }
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
        }

        return response
    }

    private fun performTokenRefresh(refreshToken: String): Boolean {
        return try {
            val jsonBody = JsonObject().apply {
                addProperty("refreshToken", refreshToken)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val refreshRequest = Request.Builder()
                .url(NetworkConstants.BASE_URL + "api/v1/auth/refresh")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val refreshResponse = client.newCall(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                val responseBodyStr = refreshResponse.body?.string()
                refreshResponse.close()
                if (!responseBodyStr.isNullOrEmpty()) {
                    val jsonObject = JsonParser.parseString(responseBodyStr).asJsonObject
                    if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull) {
                        val dataObj = jsonObject.getAsJsonObject("data")
                        val newAccessToken = if (dataObj.has("accessToken") && !dataObj.get("accessToken").isJsonNull) dataObj.get("accessToken").asString else null
                        val newRefreshToken = if (dataObj.has("refreshToken") && !dataObj.get("refreshToken").isJsonNull) dataObj.get("refreshToken").asString else refreshToken
                        val userId = if (dataObj.has("id") && !dataObj.get("id").isJsonNull) dataObj.get("id").asString else preferenceManager.getUserId() ?: ""

                        if (!newAccessToken.isNullOrEmpty()) {
                            preferenceManager.saveLogin(
                                accessToken = newAccessToken,
                                refreshToken = newRefreshToken,
                                userId = userId,
                                email = preferenceManager.getEmail()
                            )
                            return true
                        }
                    }
                }
            } else {
                refreshResponse.close()
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}