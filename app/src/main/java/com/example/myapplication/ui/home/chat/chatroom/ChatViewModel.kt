package com.example.myapplication.ui.home.chat.chatroom

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.mapper.ChatMapper
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.remote.dto.response.MessageResponse
import com.example.myapplication.data.remote.websocket.ChatSocketService
import com.example.myapplication.data.remote.websocket.StompManager
import com.example.myapplication.data.repository.MessageRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : BaseViewModel<List<Message>>(application) {

    private val messageRepository = MessageRepository(application)
    private val preferenceManager = PreferenceManager(application)

    private val stompManager = StompManager()
    private val socketService = ChatSocketService(stompManager)
    private val gson = Gson()

    val currentUserId: String = preferenceManager.getUserId() ?: ""

    private val _messages = mutableListOf<Message>()

    init {
        initWebSocket()
    }

    private fun initWebSocket() {
        val token = preferenceManager.getAccessToken() ?: ""
        if (currentUserId.isNotEmpty() && token.isNotEmpty()) {
            val wsUrl = "wss://chiikaiwa-be.onrender.com/ws/chat"
            socketService.connect(wsUrl, token)
            socketService.subscribeToChat(currentUserId)

            viewModelScope.launch {
                socketService.messageFlow.collect { (_, body) ->
                    try {
                        val messageDto = gson.fromJson(body, MessageResponse::class.java)
                        val domainMessage = ChatMapper.toDomain(messageDto)
                        _messages.add(domainMessage)
                        _uiState.value = UiState.Success(_messages.toList())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun sendRealtimeMessage(receiverId: String, text: String) {
        if (text.isBlank()) return
        socketService.sendPrivateMessage(currentUserId, receiverId, text)
    }

    fun fetchMessages(conversationId: String, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = messageRepository.getMessages(conversationId, page, size)) {
                is Resource.Success -> {
                    val pageResponse = result.data.data
                    val list = pageResponse.content.map { ChatMapper.toDomain(it) }
                    _messages.clear()
                    _messages.addAll(list)
                    _uiState.value = UiState.Success(_messages.toList())
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            when (val result = messageRepository.recallMessage(messageId)) {
                is Resource.Success -> {
                    _messages.indexOfFirst { it.id == messageId }.takeIf { it != -1 }?.let { index ->
                        _messages[index] = _messages[index].copy(isRecalled = true, content = "Tin nhắn đã được thu hồi")
                        _uiState.value = UiState.Success(_messages.toList())
                    }
                    _event.emit(UiEvent.ShowToast("Đã thu hồi tin nhắn"))
                }
                is Resource.Error -> {
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            when (val result = messageRepository.deleteMessage(messageId)) {
                is Resource.Success -> {
                    _messages.removeAll { it.id == messageId }
                    _uiState.value = UiState.Success(_messages.toList())
                    _event.emit(UiEvent.ShowToast("Đã xóa tin nhắn"))
                }
                is Resource.Error -> {
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompManager.disconnect()
    }
}
