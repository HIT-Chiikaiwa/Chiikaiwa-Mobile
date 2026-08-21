package com.example.myapplication.ui.notification

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.data.repository.notification.NotificationRepository
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    private val _notificationsState = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Idle)
    val notificationsState: StateFlow<UiState<List<NotificationDto>>> = _notificationsState

    private val _actionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val actionState: StateFlow<UiState<String>> = _actionState

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = UiState.Loading
            when (val result = repository.getNotifications(page = 0, size = 50)) {
                is Resource.Success -> {
                    val list = result.data.data?.content ?: emptyList()
                    val filteredList = filterNotificationsBySettings(list)
                    _notificationsState.value = UiState.Success(filteredList)
                }
                is Resource.Error -> {
                    _notificationsState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private fun filterNotificationsBySettings(list: List<NotificationDto>): List<NotificationDto> {
        val prefs = getApplication<Application>().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val systemNotifEnabled = prefs.getBoolean("system_notif", true)
        val appointmentNotifEnabled = prefs.getBoolean("appointment_notif", true)
        val scheduleNotifEnabled = prefs.getBoolean("schedule_notif", true)

        return list.filter { item ->
            val targetType = item.targetType?.uppercase() ?: item.type?.uppercase() ?: ""
            when (targetType) {
                "BOOKING", "APPOINTMENT" -> appointmentNotifEnabled
                "SCHEDULE", "TIMETABLE" -> scheduleNotifEnabled
                else -> systemNotifEnabled
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentList = (_notificationsState.value as? UiState.Success)?.data ?: return
        val updatedList = currentList.map { item ->
            if (item.id == notificationId) item.copy(isRead = true) else item
        }
        _notificationsState.value = UiState.Success(updatedList)

        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        val currentList = (_notificationsState.value as? UiState.Success)?.data ?: return
        if (currentList.none { it.isRead == false }) return

        val updatedList = currentList.map { it.copy(isRead = true) }
        _notificationsState.value = UiState.Success(updatedList)

        viewModelScope.launch {
            when (val result = repository.markAllNotificationsAsRead()) {
                is Resource.Success -> {
                    _actionState.value = UiState.Success(getApplication<Application>().getString(com.example.myapplication.R.string.mark_all_read_success))
                }
                is Resource.Error -> {
                    _actionState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        val currentList = (_notificationsState.value as? UiState.Success)?.data ?: return
        val updatedList = currentList.filter { it.id != notificationId }
        _notificationsState.value = UiState.Success(updatedList)

        viewModelScope.launch {
            when (val result = repository.deleteNotification(notificationId)) {
                is Resource.Success -> {
                    _actionState.value = UiState.Success(getApplication<Application>().getString(com.example.myapplication.R.string.delete_notification_success))
                }
                is Resource.Error -> {
                    _actionState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun deleteAllNotifications() {
        _notificationsState.value = UiState.Success(emptyList())

        viewModelScope.launch {
            when (val result = repository.deleteAllNotifications()) {
                is Resource.Success -> {
                    _actionState.value = UiState.Success(getApplication<Application>().getString(com.example.myapplication.R.string.delete_all_notifications_success))
                }
                is Resource.Error -> {
                    _actionState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
