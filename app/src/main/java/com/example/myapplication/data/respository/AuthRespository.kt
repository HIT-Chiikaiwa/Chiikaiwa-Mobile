package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.model.request.RegisterRequest
import com.example.myapplication.data.model.request.SendOtpRequest
import com.example.myapplication.data.model.request.VerifyOtpRequest
import com.example.myapplication.data.model.response.CommonResponse
import com.example.myapplication.data.model.response.LoginResponse
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.utils.Resource

class AuthRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return safeApiCall {
            api.login(LoginRequest(email = email, password = password))
        }
    }

    suspend fun getCurrentUser() = safeApiCall { api.getCurrentUser() }

    suspend fun register(request: RegisterRequest): Resource<CommonResponse> {
        return safeApiCall { api.register(request) }
    }

    suspend fun verifyRegisterOtp(email: String, otpCode: String): Resource<CommonResponse> {
        return safeApiCall { api.verifyRegisterOtp(VerifyOtpRequest(email, otpCode)) }
    }

    suspend fun sendOtp(email: String): Resource<CommonResponse> {
        return safeApiCall { api.sendOtp(SendOtpRequest(email)) }
    }
}