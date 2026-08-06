package com.example.myapplication.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application)

    private val _notificationsState = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Idle)
    val notificationsState: StateFlow<UiState<List<NotificationDto>>> = _notificationsState

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = UiState.Loading
            when (val result = repository.getNotifications(page = 0, size = 50)) {
                is Resource.Success -> {
                    val list = result.data.data?.content ?: emptyList()
                    _notificationsState.value = UiState.Success(list)
                }
                is Resource.Error -> {
                    _notificationsState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }
}
