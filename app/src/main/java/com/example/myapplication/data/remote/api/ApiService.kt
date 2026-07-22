package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.dto.response.LoginResponse
import com.example.myapplication.data.remote.dto.response.NearbyUserResponse
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.dto.response.SubjectDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.example.myapplication.data.model.Conversation
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Reaction

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

    @GET("chat/conversations")
    suspend fun getConversations(): Response<BaseResponse<List<Conversation>>>

    @POST("chat/conversations/direct")
    suspend fun getOrCreateDirectConversation(
        @Query("receiverId") receiverId: Long
    ): Response<BaseResponse<Conversation>>

    @POST("chat/conversations/group")
    suspend fun createGroup(
        @Body request: CreateGroupRequest
    ): Response<BaseResponse<Conversation>>

    @PUT("chat/conversations/{conversationId}")
    suspend fun updateGroup(
        @Path("conversationId") conversationId: Long,
        @Body request: UpdateGroupRequest
    ): Response<BaseResponse<Conversation>>

    @GET("chat/conversations/search")
    suspend fun searchConversations(
        @Query("keyword") keyword: String
    ): Response<BaseResponse<List<Conversation>>>

    @POST("chat/conversations/{conversationId}/members")
    suspend fun addMembers(
        @Path("conversationId") conversationId: Long,
        @Body request: AddMemberRequest
    ): Response<BaseResponse<Conversation>>

    @DELETE("chat/conversations/{conversationId}/members/{userId}")
    suspend fun removeMember(
        @Path("conversationId") conversationId: Long,
        @Path("userId") userId: Long
    ): Response<BaseResponse<CommonResponse>>

    @DELETE("chat/conversations/{conversationId}/dissolve")
    suspend fun dissolveGroup(
        @Path("conversationId") conversationId: Long
    ): Response<BaseResponse<CommonResponse>>

    @PUT("chat/conversations/{conversationId}/transfer-ownership")
    suspend fun transferOwnership(
        @Path("conversationId") conversationId: Long,
        @Query("newOwnerId") newOwnerId: Long
    ): Response<BaseResponse<Conversation>>

    @GET("chat/conversations/{conversationId}/messages")
    suspend fun getChatHistory(
        @Path("conversationId") conversationId: Long
    ): Response<BaseResponse<List<Message>>>

    @GET("chat/conversations/{conversationId}/messages/search")
    suspend fun searchMessages(
        @Path("conversationId") conversationId: Long,
        @Query("keyword") keyword: String
    ): Response<BaseResponse<List<Message>>>

    @POST("chat/messages/{messageId}/reply")
    suspend fun replyMessage(
        @Path("messageId") messageId: Long,
        @Body request: ReplyRequest
    ): Response<BaseResponse<Message>>

    @POST("chat/messages/{messageId}/forward")
    suspend fun forwardMessage(
        @Path("messageId") messageId: Long,
        @Query("targetConversationId") targetConversationId: Long
    ): Response<BaseResponse<Message>>

    @PUT("chat/messages/{messageId}/pin")
    suspend fun pinMessage(
        @Path("messageId") messageId: Long
    ): Response<BaseResponse<CommonResponse>>

    @PUT("chat/messages/{messageId}/unpin")
    suspend fun unpinMessage(
        @Path("messageId") messageId: Long
    ): Response<BaseResponse<CommonResponse>>

    @GET("chat/conversations/{conversationId}/pinned")
    suspend fun getPinnedMessages(
        @Path("conversationId") conversationId: Long
    ): Response<BaseResponse<List<Message>>>

    @POST("chat/messages/{messageId}/reactions")
    suspend fun addReaction(
        @Path("messageId") messageId: Long,
        @Query("emoji") emoji: String
    ): Response<BaseResponse<Reaction>>

    @DELETE("chat/messages/{messageId}/reactions")
    suspend fun removeReaction(
        @Path("messageId") messageId: Long
    ): Response<BaseResponse<CommonResponse>>
}