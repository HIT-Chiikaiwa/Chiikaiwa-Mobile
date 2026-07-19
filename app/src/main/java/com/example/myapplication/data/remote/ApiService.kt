package com.example.myapplication.data.remote

import com.example.myapplication.data.model.request.*
import com.example.myapplication.data.model.response.BaseResponse
import com.example.myapplication.data.model.response.CommonResponse
import com.example.myapplication.data.model.response.LoginResponse
import com.example.myapplication.data.model.response.NearbyUserResponse
import com.example.myapplication.data.model.response.UserDto
import com.example.myapplication.data.model.response.SubjectDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/user/current")
    suspend fun getCurrentUser(): Response<BaseResponse<UserDto>>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseResponse<CommonResponse>>

    @POST("api/v1/auth/verify-register-otp")
    suspend fun verifyRegisterOtp(@Body request: VerifyOtpRequest): Response<BaseResponse<CommonResponse>>

    @POST("api/v1/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<BaseResponse<CommonResponse>>

    @POST("api/v1/auth/forgot-password/send-otp")
    suspend fun forgotPasswordSendOtp(@Body request: SendOtpRequest): Response<BaseResponse<CommonResponse>>

    @POST("api/v1/auth/forgot-password/verify-otp")
    suspend fun forgotPasswordVerifyOtp(@Body request: VerifyOtpRequest): Response<BaseResponse<CommonResponse>>

    @POST("api/v1/auth/forgot-password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<BaseResponse<CommonResponse>>

    @GET("api/v1/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<BaseResponse<UserDto>>

    @PATCH("api/v1/profile/{userId}/status")
    suspend fun toggleBuddyStatus(
        @Path("userId") userId: String,
        @Body request: ToggleBuddyStatusRequest
    ): Response<BaseResponse<UserDto>>

    @PUT("api/v1/profile/{userId}/password")
    suspend fun changePassword(
        @Path("userId") userId: String,
        @Body request: ChangePasswordRequest
    ): Response<BaseResponse<CommonResponse>>

    @GET("api/v1/profile/{userId}/subjects")
    suspend fun getSubjects(
        @Path("userId") userId: String,
        @Query("type") type: String? = null
    ): Response<BaseResponse<List<SubjectDto>>>

    @POST("api/v1/profile/{userId}/subjects")
    suspend fun addSubject(
        @Path("userId") userId: String,
        @Body request: AddSubjectRequest
    ): Response<BaseResponse<SubjectDto>>

    @DELETE("api/v1/profile/{userId}/subjects/{subjectId}")
    suspend fun deleteSubject(
        @Path("userId") userId: String,
        @Path("subjectId") subjectId: String
    ): Response<BaseResponse<CommonResponse>>

    @DELETE("api/v1/profile/{userId}")
    suspend fun deleteAccount(
        @Path("userId") userId: String
    ): Response<BaseResponse<CommonResponse>>
    @GET("api/v1/location/radar")
    suspend fun getNearbyUsers(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double
    ): Response<BaseResponse<List<NearbyUserResponse>>>
}