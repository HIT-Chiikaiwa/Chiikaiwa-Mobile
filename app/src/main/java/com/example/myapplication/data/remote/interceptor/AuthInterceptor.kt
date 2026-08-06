package com.example.myapplication.data.remote.interceptor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.ui.auth.LoginActivity
import okhttp3.Interceptor
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

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401 && !isAuthEndpoint) {
            synchronized(this) {
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

}