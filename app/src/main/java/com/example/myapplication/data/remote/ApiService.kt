package com.example.myapplication.data.remote

import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.model.request.RegisterRequest
import com.example.myapplication.data.model.request.SendOtpRequest
import com.example.myapplication.data.model.request.VerifyOtpRequest
import com.example.myapplication.data.model.response.CommonResponse
import com.example.myapplication.data.model.response.LoginResponse
import com.example.myapplication.data.model.response.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/user/current") suspend fun getCurrentUser(): Response<UserDto>

    @POST("api/v1/auth/register") suspend fun register(@Body request: RegisterRequest): Response<CommonResponse>

    @POST("api/v1/auth/verify-register-otp") suspend fun verifyRegisterOtp(@Body request: VerifyOtpRequest): Response<CommonResponse>

    @POST("api/v1/auth/send-otp") suspend fun sendOtp(@Body request: SendOtpRequest): Response<CommonResponse>
}