package com.example.myapplication.ui.home.chat.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.mapper.ChatMapper
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageStatus
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.data.model.User
import com.example.myapplication.data.remote.network.NetworkConstants
import com.example.myapplication.data.remote.websocket.ChatSocketService
import com.example.myapplication.data.remote.websocket.StompManager
import com.example.myapplication.data.repository.ConversationRepository
import com.example.myapplication.data.repository.MessageRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ChatViewModel(application: Application) : BaseViewModel<List<Message>>(application) {

    private val messageRepository = MessageRepository(application)
    private val conversationRepository = ConversationRepository(application)
    private val preferenceManager = PreferenceManager(application)
    private val socketService = ChatSocketService(StompManager())
    private val messageParser = ChatMessageParser()

    val currentUserId: String = preferenceManager.getUserId() ?: ""
    private var activeConversationId: String = ""
    private val _messages = mutableListOf<Message>()

    init {
        initWebSocket()
    }

    private fun updateState() {
        _uiState.value = UiState.Success(_messages.toList())
    }

    private fun initWebSocket() {
        val token = preferenceManager.getAccessToken() ?: ""
        if (currentUserId.isEmpty() || token.isEmpty()) return

        socketService.connect("${NetworkConstants.WS_URL}?token=$token", token)
        socketService.subscribeToChat(currentUserId, activeConversationId)

        viewModelScope.launch {
            socketService.messageFlow.collect { (_, body) ->
                parseIncomingWebSocketMessage(body)
            }
        }
    }

    private fun parseIncomingWebSocketMessage(body: String) {
        try {
            val incomingMsg = messageParser.parseWsMessage(body, activeConversationId) ?: return

            _messages.removeAll { 
                it.id == incomingMsg.id || (it.id.startsWith("temp_") && it.content == incomingMsg.content)
            }
            _messages.add(incomingMsg)
            updateState()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error parsing WS message", e)
        }
    }

    fun initChatSession(convId: String, targetId: String) {
        if (convId.isNotEmpty()) {
            activeConversationId = convId
            socketService.subscribeToChat(currentUserId, activeConversationId)
            fetchMessages(convId)
        } else if (targetId.isNotEmpty()) {
            reInitWithTargetId(targetId)
        }
    }

    private fun reInitWithTargetId(targetId: String, pendingMessage: String? = null) {
        viewModelScope.launch {
            when (val result = conversationRepository.createOrGetDirectConversation(targetId)) {
                is Resource.Success -> {
                    activeConversationId = result.data.data.id
                    socketService.subscribeToChat(currentUserId, activeConversationId)
                    fetchMessages(activeConversationId)
                    if (!pendingMessage.isNullOrBlank()) {
                        sendRealtimeMessage(targetId, pendingMessage)
                    }
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun sendRealtimeMessage(targetId: String, text: String) {
        if (text.isBlank()) return
        val msgText = text.trim()

        val tempMsg = Message(
            id = "temp_${System.currentTimeMillis()}",
            conversationId = activeConversationId,
            sender = User(id = currentUserId, fullName = "Tôi", avatar = null),
            content = msgText,
            type = MessageType.TEXT,
            status = MessageStatus.SENT,
            createdAt = "Vừa xong",
            updatedAt = "",
            isRecalled = false
        )

        _messages.add(tempMsg)
        updateState()

        if (activeConversationId.isEmpty() && targetId.isNotEmpty() && targetId != currentUserId) {
            reInitWithTargetId(targetId, pendingMessage = null)
            return
        }

        socketService.sendMessage(activeConversationId, msgText)
    }

    fun sendImageMessage(file: MultipartBody.Part) {
        if (activeConversationId.isEmpty()) return

        val tempMsg = Message(
            id = "temp_${System.currentTimeMillis()}",
            conversationId = activeConversationId,
            sender = User(id = currentUserId, fullName = "Tôi", avatar = null),
            content = "",
            type = MessageType.IMAGE,
            status = MessageStatus.SENT,
            createdAt = "Vừa xong",
            updatedAt = "",
            isRecalled = false
        )
        _messages.add(tempMsg)
        updateState()

        viewModelScope.launch {
            when (val result = messageRepository.uploadImage(activeConversationId, file)) {
                is Resource.Success -> {
                    val imageUrl = messageParser.extractImageUrl(result.data.data)
                    val idx = _messages.indexOfFirst { it.id == tempMsg.id }
                    if (idx != -1 && imageUrl.isNotEmpty()) {
                        _messages[idx] = tempMsg.copy(content = imageUrl)
                        updateState()
                        socketService.sendMessage(activeConversationId, imageUrl, type = "IMAGE")
                    }
                }
                is Resource.Error -> {
                    _messages.removeAll { it.id == tempMsg.id }
                    updateState()
                    _event.emit(UiEvent.ShowToast("Gửi ảnh thất bại: ${result.message}"))
                }
            }
        }
    }

    fun fetchMessages(conversationId: String, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            when (val result = messageRepository.getMessages(conversationId, page, size)) {
                is Resource.Success -> {
                    try {
                        val rawList = result.data.data.content.map { ChatMapper.toDomain(it) }

                        val now = System.currentTimeMillis()
                        val recentTemps = _messages.filter { temp ->
                            if (!temp.id.startsWith("temp_")) return@filter false
                            val tempTime = temp.id.substringAfter("temp_").toLongOrNull() ?: 0L
                            (now - tempTime) < 15000
                        }

                        _messages.clear()
                        _messages.addAll(rawList.reversed())

                        recentTemps.forEach { temp ->
                            if (_messages.none { it.id == temp.id }) {
                                _messages.add(temp)
                            }
                        }

                        updateState()
                        if (conversationId.isNotEmpty()) socketService.sendReadReceipt(conversationId)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error parsing messages response", e)
                        updateState()
                    }
                }
                is Resource.Error -> updateState()
            }
        }
    }

    fun recallMessage(messageId: String) {
        viewModelScope.launch {
            if (messageRepository.recallMessage(messageId) is Resource.Success) {
                val index = _messages.indexOfFirst { it.id == messageId }
                if (index != -1) {
                    _messages[index] = _messages[index].copy(isRecalled = true, content = "Tin nhắn đã được thu hồi")
                    updateState()
                }
                _event.emit(UiEvent.ShowToast("Đã thu hồi tin nhắn"))
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            if (messageRepository.deleteMessage(messageId) is Resource.Success) {
                _messages.removeAll { it.id == messageId }
                updateState()
                _event.emit(UiEvent.ShowToast("Đã xóa tin nhắn"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.disconnect()
    }
}
