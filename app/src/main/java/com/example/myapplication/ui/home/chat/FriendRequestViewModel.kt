package com.example.myapplication.ui.home.chat

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.FriendDto
import com.example.myapplication.data.remote.dto.response.UserSearchDto
import com.example.myapplication.data.remote.websocket.WebSocketManager
import com.example.myapplication.data.repository.FriendRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class FriendRequestViewModel(application: Application) : BaseViewModel<Unit>(application) {

    private val friendRepository = FriendRepository(application)
    private val socketService = WebSocketManager

    fun searchUsers(keyword: String, onResult: (List<UserSearchDto>) -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.searchUsers(keyword)) {
                is Resource.Success -> {
                    onResult(result.data?.data ?: emptyList())
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    onResult(emptyList())
                }
            }
        }
    }

    fun sendFriendRequest(targetUserId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.sendFriendRequest(targetUserId)) {
                is Resource.Success -> {
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun getPendingFriendRequests(onResult: (List<FriendDto>) -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.getPendingFriendRequests()) {
                is Resource.Success -> {
                    onResult(result.data?.data?.content ?: emptyList())
                }
                is Resource.Error -> {
                    onResult(emptyList())
                }
            }
        }
    }

    fun acceptFriendRequest(requestId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.acceptFriendRequest(requestId)) {
                is Resource.Success -> {
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun rejectFriendRequest(requestId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.rejectFriendRequest(requestId)) {
                is Resource.Success -> {
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun unfriend(friendId: String, conversationId: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = friendRepository.unfriend(friendId)) {
                is Resource.Success -> {
                    if (!conversationId.isNullOrEmpty()) {
                        socketService.sendMessage(conversationId, "UNFRIEND", "UNFRIEND")
                    }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
