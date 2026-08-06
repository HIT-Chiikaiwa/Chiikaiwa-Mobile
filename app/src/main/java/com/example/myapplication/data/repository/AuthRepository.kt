package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.dto.response.LoginResponse
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import com.example.myapplication.data.remote.api.ApiService

class AuthRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return safeApiCall {
            api.login(LoginRequest(email = email, password = password))
        }
    }

    suspend fun register(request: RegisterRequest): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.register(request) }
    }

    suspend fun verifyRegisterOtp(email: String, otpCode: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.verifyRegisterOtp(VerifyOtpRequest(email, otpCode)) }
    }

    suspend fun sendOtp(email: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.sendOtp(SendOtpRequest(email)) }
    }

    suspend fun forgotPasswordSendOtp(email: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.forgotPasswordSendOtp(SendOtpRequest(email)) }
    }

    suspend fun forgotPasswordVerifyOtp(email: String, otpCode: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.forgotPasswordVerifyOtp(VerifyOtpRequest(email, otpCode)) }
    }

    suspend fun resetPassword(email: String, pass: String, confirmPass: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.resetPassword(ResetPasswordRequest(email, pass, confirmPass)) }
    }

    suspend fun refreshToken(refreshToken: String): Resource<LoginResponse> {
        return safeApiCall { api.refreshToken(RefreshTokenRequest(refreshToken)) }
    }

    suspend fun logout(refreshToken: String): Resource<BaseResponse<Any>> {
        return safeApiCall { api.logout(LogoutRequest(refreshToken)) }
    }
}