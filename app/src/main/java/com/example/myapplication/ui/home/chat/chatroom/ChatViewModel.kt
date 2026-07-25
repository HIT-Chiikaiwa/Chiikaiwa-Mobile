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
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : BaseViewModel<List<Message>>(application) {

    private val messageRepository = MessageRepository(application)
    private val conversationRepository = ConversationRepository(application)
    private val preferenceManager = PreferenceManager(application)
    private val socketService = ChatSocketService(StompManager())
    private val gson = Gson()

    val currentUserId: String = preferenceManager.getUserId() ?: ""
    private var activeConversationId: String = ""
    private var activeTargetUserId: String = ""
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
            val jsonObj = gson.fromJson(body, JsonObject::class.java) ?: return
            val data = if (jsonObj.has("data") && jsonObj.get("data").isJsonObject) {
                jsonObj.getAsJsonObject("data")
            } else jsonObj

            val content = data.get("content")?.asString ?: data.get("text")?.asString ?: ""
            if (content.isEmpty()) return

            val msgId = data.get("id")?.asString ?: data.get("messageId")?.asString ?: System.currentTimeMillis().toString()
            val senderId = data.get("senderId")?.asString
                ?: data.getAsJsonObject("sender")?.get("id")?.asString ?: ""
            val senderName = data.get("senderName")?.asString
                ?: data.getAsJsonObject("sender")?.get("fullName")?.asString ?: "Người dùng"
            val convId = data.get("conversationId")?.asString ?: activeConversationId

            if (senderId.isNotEmpty() && senderId != currentUserId) {
                activeTargetUserId = senderId
            }

            val incomingMsg = Message(
                id = msgId,
                conversationId = convId,
                sender = User(id = senderId, fullName = senderName, avatar = null),
                content = content,
                type = MessageType.TEXT,
                status = MessageStatus.SENT,
                createdAt = "Vừa xong",
                updatedAt = "",
                isRecalled = false
            )

            _messages.removeAll { it.id == msgId || (it.content == content && (it.sender.id == senderId || it.sender.id == currentUserId)) }
            _messages.add(incomingMsg)
            updateState()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error parsing WS message", e)
        }
    }

    fun initChatSession(convId: String, targetId: String) {
        if (targetId.isNotEmpty() && targetId != currentUserId) {
            activeTargetUserId = targetId
        }
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
            _uiState.value = UiState.Loading
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

        if (activeConversationId.isEmpty() && targetId.isNotEmpty() && targetId != currentUserId) {
            reInitWithTargetId(targetId, pendingMessage = msgText)
            return
        }

        val receiverId = when {
            activeTargetUserId.isNotEmpty() && activeTargetUserId != currentUserId -> activeTargetUserId
            targetId.isNotEmpty() && targetId != currentUserId && targetId != activeConversationId -> targetId
            else -> targetId
        }

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

        socketService.sendMessage(currentUserId, receiverId, msgText, activeConversationId)

        if (activeConversationId.isNotEmpty()) {
            viewModelScope.launch {
                delay(600)
                fetchMessages(activeConversationId)
            }
        }
    }

    fun fetchMessages(conversationId: String, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            when (val result = messageRepository.getMessages(conversationId, page, size)) {
                is Resource.Success -> {
                    try {
                        val rawList = result.data.data.content.map { ChatMapper.toDomain(it) }

                        val otherMsg = rawList.firstOrNull { it.sender.id.isNotEmpty() && it.sender.id != currentUserId }
                        if (otherMsg != null) {
                            activeTargetUserId = otherMsg.sender.id
                        }

                        val localTemps = _messages.filter { it.id.startsWith("temp_") }

                        _messages.clear()
                        _messages.addAll(rawList.reversed())

                        localTemps.forEach { temp ->
                            if (_messages.none { it.content == temp.content && it.sender.id == temp.sender.id }) {
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
        socketService.connect("", "")
    }
}
