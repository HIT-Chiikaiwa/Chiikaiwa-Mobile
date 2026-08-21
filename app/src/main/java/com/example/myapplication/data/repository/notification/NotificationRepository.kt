package com.example.myapplication.data.repository.notification
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.DeleteNotificationsBatchRequest
import com.example.myapplication.data.remote.dto.request.RegisterDeviceRequest
import com.example.myapplication.data.remote.dto.response.ActionStatusDto
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.data.remote.dto.response.PageResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class NotificationRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getNotifications(
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): Resource<BaseResponse<PageResponse<NotificationDto>>> {
        return safeApiCall { api.getNotifications(page, size, sort) }
    }

    suspend fun getUnreadNotificationCount(): Resource<BaseResponse<Int>> {
        return safeApiCall { api.getUnreadNotificationCount() }
    }

    suspend fun markNotificationAsRead(notificationId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.markNotificationAsRead(notificationId) }
    }

    suspend fun markAllNotificationsAsRead(): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.markAllNotificationsAsRead() }
    }

    suspend fun registerDevice(
        fcmToken: String,
        deviceName: String? = null
    ): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.registerDevice(RegisterDeviceRequest(fcmToken, "ANDROID", deviceName)) }
    }

    suspend fun unregisterDevice(fcmToken: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.unregisterDevice(fcmToken) }
    }

    suspend fun deleteNotification(notificationId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.deleteNotification(notificationId) }
    }

    suspend fun deleteNotificationsBatch(notificationIds: List<String>): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.deleteNotificationsBatch(DeleteNotificationsBatchRequest(notificationIds)) }
    }

    suspend fun deleteAllNotifications(): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.deleteAllNotifications() }
    }
}
