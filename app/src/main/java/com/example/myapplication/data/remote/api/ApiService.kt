package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

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

    @Multipart
    @POST("api/v1/profile/{userId}/avatar")
    suspend fun uploadAvatar(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<UserDto>>

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

    @GET("api/v1/chat/conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<ConversationResponse>>>

    @GET("api/v1/chat/conversations/search")
    suspend fun searchConversations(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<ConversationResponse>>>

    @POST("api/v1/chat/conversations/direct")
    suspend fun createOrGetDirectConversation(
        @Body request: DirectChatRequest
    ): Response<BaseResponse<ConversationResponse>>

    @POST("api/v1/chat/conversations/group")
    suspend fun createGroup(
        @Body request: CreateGroupRequest
    ): Response<BaseResponse<ConversationResponse>>

    @PUT("api/v1/chat/conversations/{id}")
    suspend fun updateGroup(
        @Path("id") id: String,
        @Body request: UpdateGroupRequest
    ): Response<BaseResponse<ConversationResponse>>

    @POST("api/v1/chat/conversations/{id}/members")
    suspend fun addMembers(
        @Path("id") id: String,
        @Body request: AddMemberRequest
    ): Response<BaseResponse<ActionStatusDto>>

    @DELETE("api/v1/chat/conversations/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Response<BaseResponse<ActionStatusDto>>

    @PUT("api/v1/chat/conversations/{id}/transfer-ownership")
    suspend fun transferOwnership(
        @Path("id") id: String,
        @Query("newOwnerId") newOwnerId: String
    ): Response<BaseResponse<ActionStatusDto>>

    @DELETE("api/v1/chat/conversations/{id}/dissolve")
    suspend fun dissolveGroup(
        @Path("id") id: String
    ): Response<BaseResponse<ActionStatusDto>>

    @GET("api/v1/chat/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<MessageResponse>>>

    @GET("api/v1/chat/conversations/{id}/messages/search")
    suspend fun searchMessages(
        @Path("id") id: String,
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<MessageResponse>>>

    @PUT("api/v1/chat/messages/{msgId}/recall")
    suspend fun recallMessage(
        @Path("msgId") msgId: String
    ): Response<BaseResponse<Any>>

    @DELETE("api/v1/chat/messages/{msgId}")
    suspend fun deleteMessage(
        @Path("msgId") msgId: String
    ): Response<BaseResponse<Any>>

    @Multipart
    @POST("api/v1/chat/conversations/{id}/upload")
    suspend fun uploadImage(
        @Path("id") conversationId: String,
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<String>>

    @GET("api/v1/users/search/phone")
    suspend fun searchUserByPhone(
        @Query("phone") phone: String
    ): Response<BaseResponse<UserSearchDto>>

    @POST("api/v1/friends/request/{targetUserId}")
    suspend fun sendFriendRequest(
        @Path("targetUserId") targetUserId: String
    ): Response<BaseResponse<FriendActionResponse>>

    @PUT("api/v1/friends/request/{requestId}/accept")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: String
    ): Response<BaseResponse<FriendActionResponse>>

    @PUT("api/v1/friends/request/{requestId}/reject")
    suspend fun rejectFriendRequest(
        @Path("requestId") requestId: String
    ): Response<BaseResponse<FriendActionResponse>>

    @GET("api/v1/friends")
    suspend fun getFriends(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<FriendDto>>>

    @GET("api/v1/friends/search")
    suspend fun searchFriends(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<FriendDto>>>

    @GET("api/v1/friends/requests/pending")
    suspend fun getPendingFriendRequests(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<FriendDto>>>

    @DELETE("api/v1/friends/{friendId}")
    suspend fun unfriend(
        @Path("friendId") friendId: String
    ): Response<BaseResponse<FriendActionResponse>>
}