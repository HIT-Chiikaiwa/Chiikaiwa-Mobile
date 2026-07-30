package com.example.myapplication.ui.home.chat

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.remote.network.NetworkConstants
import com.example.myapplication.data.remote.websocket.ChatSocketService
import com.example.myapplication.data.remote.websocket.StompManager
import com.example.myapplication.data.repository.ConversationRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class FriendsListViewModel(application: Application) : BaseViewModel<List<ConversationResponse>>(application) {

    private val conversationRepository = ConversationRepository(application)
    private val preferenceManager = PreferenceManager(application)

    private val stompManager = StompManager()
    private val socketService = ChatSocketService(stompManager)

    init {
        initWebSocket()
        fetchConversations()
    }

    private fun initWebSocket() {
        val currentUserId = preferenceManager.getUserId() ?: ""
        val token = preferenceManager.getAccessToken() ?: ""
        if (currentUserId.isNotEmpty() && token.isNotEmpty()) {
            val wsUrl = "${NetworkConstants.WS_URL}?token=$token"
            socketService.connect(wsUrl, token)
            socketService.subscribeToChat(currentUserId)

            viewModelScope.launch {
                socketService.messageFlow.collect {
                    fetchConversations()
                }
            }
        }
    }

    fun fetchConversations() {
        viewModelScope.launch {
            when (val result = conversationRepository.getConversations()) {
                is Resource.Success -> {
                    val list = result.data?.data?.content ?: emptyList()
                    _uiState.value = UiState.Success(list)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun startDirectChat(targetUserId: String, onResult: (convId: String, userName: String) -> Unit) {
        viewModelScope.launch {
            when (val result = conversationRepository.createOrGetDirectConversation(targetUserId)) {
                is Resource.Success -> {
                    val conv = result.data?.data
                    val convId = conv?.id ?: ""
                    val userName = conv?.groupName ?: conv?.lastMessage?.senderName ?: "Người dùng"
                    if (convId.isNotEmpty()) {
                        onResult(convId, userName)
                    } else {
                        _uiState.value = UiState.Error("Không thể tạo cuộc hội thoại")
                    }
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private val friendRepository = com.example.myapplication.data.repository.FriendRepository(application)

    fun searchUsers(keyword: String, onResult: (List<com.example.myapplication.data.remote.dto.response.UserSearchDto>) -> Unit) {
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

    fun searchConversations(keyword: String) {
        if (keyword.isBlank()) {
            fetchConversations()
            return
        }
        viewModelScope.launch {
            when (val result = conversationRepository.searchConversations(keyword)) {
                is Resource.Success -> {
                    val list = result.data?.data?.content ?: emptyList()
                    _uiState.value = UiState.Success(list)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompManager.disconnect()
    }
}
