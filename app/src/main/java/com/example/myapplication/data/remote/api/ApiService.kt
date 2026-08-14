package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

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

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<BaseResponse<Any>>

    @GET("api/v1/user/current")
    suspend fun getCurrentUser(): Response<BaseResponse<UserDto>>

    @GET("api/v1/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<BaseResponse<UserDto>>

    @PUT("api/v1/profile/{userId}/status-tag")
    suspend fun updateStatusTag(
        @Path("userId") userId: String,
        @Body request: UpdateStatusTagRequest
    ): Response<BaseResponse<UserDto>>

    @PUT("api/v1/profile/{userId}/personal-info")
    suspend fun updatePersonalInfo(
        @Path("userId") userId: String,
        @Body request: UpdatePersonalInfoRequest
    ): Response<BaseResponse<UserDto>>

    @PUT("api/v1/profile/{userId}/academic-info")
    suspend fun updateAcademicInfo(
        @Path("userId") userId: String,
        @Body request: UpdateAcademicInfoRequest
    ): Response<BaseResponse<UserDto>>

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

    @PUT("api/v1/location/update")
    suspend fun updateLocation(
        @Body request: UpdateLocationRequest
    ): Response<BaseResponse<CommonResponse>>

    @DELETE("api/v1/location/remove")
    suspend fun removeLocation(): Response<BaseResponse<CommonResponse>>

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
    ): Response<BaseResponse<Any>>

    @POST("api/v1/chat/conversations/{id}/schedule-invite")
    suspend fun scheduleInvite(
        @Path("id") conversationId: String,
        @Body request: ScheduleInviteRequest
    ): Response<BaseResponse<Any>>

    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @Query("keyword") keyword: String
    ): Response<BaseResponse<List<UserSearchDto>>>

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

    @POST("api/v1/bookings/conversation/{conversationId}")
    suspend fun createBookingInConversation(
        @Path("conversationId") conversationId: String,
        @Body request: CreateBookingRequest
    ): Response<BaseResponse<BookingDto>>

    @GET("api/v1/bookings")
    suspend fun getMyBookings(): Response<BaseResponse<List<BookingDto>>>

    @GET("api/v1/bookings/weekly")
    suspend fun getWeeklyBookings(
        @Query("weekStart") weekStart: String
    ): Response<BaseResponse<WeeklyBookingResponse>>

    @GET("api/v1/bookings/{bookingId}")
    suspend fun getBookingDetail(
        @Path("bookingId") bookingId: String
    ): Response<BaseResponse<BookingDto>>

    @PUT("api/v1/bookings/{bookingId}/accept")
    suspend fun acceptBooking(
        @Path("bookingId") bookingId: String
    ): Response<BaseResponse<BookingDto>>

    @PUT("api/v1/bookings/{bookingId}/reject")
    suspend fun rejectBooking(
        @Path("bookingId") bookingId: String
    ): Response<BaseResponse<BookingDto>>

    @PATCH("api/v1/bookings/{bookingId}/complete")
    suspend fun completeBooking(
        @Path("bookingId") bookingId: String
    ): Response<BaseResponse<BookingDto>>

    @PATCH("api/v1/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: String,
        @Body request: CancelBookingRequest
    ): Response<BaseResponse<BookingDto>>

    @POST("api/v1/bookings/{bookingId}/rate")
    suspend fun rateBooking(
        @Path("bookingId") bookingId: String,
        @Body request: RateBookingRequest
    ): Response<BaseResponse<ActionStatusDto>>

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<BaseResponse<PageResponse<NotificationDto>>>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<BaseResponse<Int>>

    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String
    ): Response<BaseResponse<ActionStatusDto>>

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<BaseResponse<ActionStatusDto>>

    @POST("api/v1/devices")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<BaseResponse<ActionStatusDto>>

    @DELETE("api/v1/devices")
    suspend fun unregisterDevice(
        @Query("fcmToken") fcmToken: String
    ): Response<BaseResponse<ActionStatusDto>>

    @DELETE("api/v1/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: String
    ): Response<BaseResponse<ActionStatusDto>>

    @HTTP(method = "DELETE", path = "api/v1/notifications/batch", hasBody = true)
    suspend fun deleteNotificationsBatch(
        @Body request: DeleteNotificationsBatchRequest
    ): Response<BaseResponse<ActionStatusDto>>

    @DELETE("api/v1/notifications/all")
    suspend fun deleteAllNotifications(): Response<BaseResponse<ActionStatusDto>>

    @GET("api/v1/user/{userId}/online-status")
    suspend fun getUserOnlineStatus(
        @Path("userId") userId: String
    ): Response<BaseResponse<OnlineStatusDto>>

    @POST("api/v1/users/block/{userId}")
    suspend fun blockUser(
        @Path("userId") userId: String
    ): Response<BaseResponse<CommonResponse>>

    @DELETE("api/v1/users/block/{userId}")
    suspend fun unblockUser(
        @Path("userId") userId: String
    ): Response<BaseResponse<CommonResponse>>

    @GET("api/v1/users/blocked")
    suspend fun getBlockedUsers(): Response<BaseResponse<List<BlockedUserDto>>>
}