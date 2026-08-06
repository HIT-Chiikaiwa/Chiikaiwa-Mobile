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
import com.example.myapplication.data.remote.websocket.WebSocketManager
import com.example.myapplication.data.remote.dto.request.ScheduleInviteRequest
import com.example.myapplication.data.repository.ConversationRepository
import com.example.myapplication.data.repository.MessageRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ChatViewModel(application: Application) : BaseViewModel<List<Message>>(application) {

    private val messageRepository = MessageRepository(application)
    private val conversationRepository = ConversationRepository(application)
    private val preferenceManager = PreferenceManager(application)
    private val socketService = WebSocketManager
    private val messageParser = ChatMessageParser()
    private val bookingRepository = BookingRepository(application)
    private val bookingManager = ChatBookingManager()

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
            Log.d("CHAT_REALTIME_LOG", "[UI_RECEIVED_WS_RAW] $body")
            val gson = com.google.gson.Gson()
            val rawJsonObj = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val dataObj = if (rawJsonObj.has("data") && rawJsonObj.get("data")?.isJsonObject == true) {
                rawJsonObj.getAsJsonObject("data")
            } else rawJsonObj

            var directBookingId = dataObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
            var directStatus = dataObj.get("status")?.takeIf { !it.isJsonNull }?.asString
            var directReason = dataObj.get("cancelReason")?.takeIf { !it.isJsonNull }?.asString

            val contentStr = dataObj.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
            if (directBookingId.isNullOrEmpty() && contentStr.isNotEmpty()) {
                try {
                    val contentJson = gson.fromJson(contentStr, com.google.gson.JsonObject::class.java)
                    if (contentJson != null && contentJson.has("bookingId")) {
                        directBookingId = contentJson.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
                        if (directStatus.isNullOrEmpty()) {
                            directStatus = contentJson.get("status")?.takeIf { !it.isJsonNull }?.asString
                        }
                        if (directReason.isNullOrEmpty()) {
                            directReason = contentJson.get("cancelReason")?.takeIf { !it.isJsonNull }?.asString
                        }
                    }
                } catch (e: Exception) {
                }
            }

            val isStatusUpdateOnly = !directBookingId.isNullOrEmpty() && !directStatus.isNullOrEmpty() &&
                    !contentStr.contains("scheduledAt") && !dataObj.has("scheduledAt")

            if (!directBookingId.isNullOrEmpty() && !directStatus.isNullOrEmpty()) {
                bookingManager.updateBookingMessageStatus(_messages, directBookingId, directStatus, directReason)
                if (isStatusUpdateOnly) {
                    updateState()
                    return
                }
            }

            val incomingMsg = messageParser.parseWsMessage(body, activeConversationId)
            if (incomingMsg != null) {
                if (incomingMsg.type == MessageType.SYSTEM) {
                    bookingManager.processIncomingSystemMessage(_messages, incomingMsg)
                    val isBookingStatusNotification = com.example.myapplication.utils.BookingMessageHelper.extractStatusFromSystemMessage(incomingMsg.content) != null
                    if (isBookingStatusNotification) {
                        updateState()
                        return
                    }
                } else {
                    bookingManager.processIncomingSystemMessage(_messages, incomingMsg)
                }

                _messages.removeAll { 
                    it.id == incomingMsg.id || (it.id.startsWith("temp_") && it.content == incomingMsg.content)
                }
                _messages.add(incomingMsg)
            }

            updateState()
        } catch (e: Exception) {
            Log.e("CHAT_REALTIME_LOG", "[UI_PARSE_ERROR] Error parsing WS message", e)
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

    fun sendImageMessage(file: MultipartBody.Part, localImagePath: String = "") {
        if (activeConversationId.isEmpty()) return

        val tempMsg = Message(
            id = "temp_${System.currentTimeMillis()}",
            conversationId = activeConversationId,
            sender = User(id = currentUserId, fullName = "Tôi", avatar = null),
            content = localImagePath,
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

    fun updateBookingMessageRating(bookingId: String, score: Int) {
        if (bookingManager.updateBookingMessageRating(_messages, bookingId, score)) {
            updateState()
        }
    }

    fun updateBookingMessageStatus(bookingId: String, newStatus: String, reason: String? = null) {
        if (bookingManager.updateBookingMessageStatus(_messages, bookingId, newStatus, reason)) {
            updateState()
        }
    }

    fun fetchMessages(conversationId: String = activeConversationId, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            when (val result = messageRepository.getMessages(conversationId, page, size)) {
                is Resource.Success -> {
                    try {
                        val rawList = result.data.data.content.map { ChatMapper.toDomain(it) }
                        val processedList = bookingManager.processMessageList(rawList)

                        val now = System.currentTimeMillis()
                        val recentTemps = _messages.filter { temp ->
                            if (!temp.id.startsWith("temp_")) return@filter false
                            val tempTime = temp.id.substringAfter("temp_").toLongOrNull() ?: 0L
                            (now - tempTime) < 15000
                        }

                        _messages.clear()
                        _messages.addAll(processedList.reversed())

                        recentTemps.forEach { temp ->
                            if (_messages.none { it.id == temp.id }) {
                                _messages.add(temp)
                            }
                        }

                        syncBookingStatuses(processedList)
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

    private suspend fun syncBookingStatuses(messages: List<Message>) {
        Log.d("CHAT_BOOKING_DEBUG", "[SYNC_START] syncBookingStatuses called with ${messages.size} messages")
        val gson = com.google.gson.Gson()
        val bookingIds = mutableSetOf<String>()
        for (msg in messages) {
            if (msg.type == MessageType.BOOKING || com.example.myapplication.utils.BookingMessageHelper.isBookingMessage(msg.content)) {
                try {
                    val jsonObj = gson.fromJson(msg.content, com.google.gson.JsonObject::class.java)
                    val bId = jsonObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
                    if (!bId.isNullOrEmpty()) {
                        bookingIds.add(bId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Log.d("CHAT_BOOKING_DEBUG", "[SYNC_BOOKINGS_FOUND] Extracted bookingIds: $bookingIds")
        if (bookingIds.isEmpty()) return

        var updatedAny = false
        coroutineScope {
            bookingIds.map { bId ->
                async {
                    Log.d("CHAT_BOOKING_DEBUG", "[SYNC_FETCH] Fetching getBookingDetail for bId=$bId")
                    when (val result = bookingRepository.getBookingDetail(bId)) {
                        is Resource.Success -> {
                            val booking = result.data.data
                            val realStatus = booking.status
                            Log.d("CHAT_BOOKING_DEBUG", "[SYNC_SUCCESS] bId=$bId realStatus=$realStatus cancelReason=${booking.cancelReason}")
                            if (!realStatus.isNullOrEmpty()) {
                                if (bookingManager.updateBookingMessageStatus(_messages, bId, realStatus, booking.cancelReason)) {
                                    updatedAny = true
                                    Log.d("CHAT_BOOKING_DEBUG", "[SYNC_UPDATED] Updated bId=$bId in _messages to $realStatus")
                                } else {
                                    Log.w("CHAT_BOOKING_DEBUG", "[SYNC_NO_MATCH] updateBookingMessageStatus returned false for bId=$bId")
                                }
                            }
                            if (booking.hasRated == true && booking.myRating != null) {
                                if (bookingManager.updateBookingMessageRating(_messages, bId, booking.myRating!!)) {
                                    updatedAny = true
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.e("CHAT_BOOKING_DEBUG", "[SYNC_ERROR] getBookingDetail failed for bId=$bId: ${result.message}")
                        }
                    }
                }
            }.awaitAll()
        }

        if (updatedAny) {
            Log.d("CHAT_BOOKING_DEBUG", "[SYNC_RE_RENDER] Emitting new UI state after syncing booking statuses")
            updateState()
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

    fun notifyBookingStatusChanged(bookingId: String, newStatus: String, reason: String? = null) {
        if (activeConversationId.isEmpty()) return
        val jsonPayload = com.google.gson.JsonObject().apply {
            addProperty("bookingId", bookingId)
            addProperty("status", newStatus)
            if (!reason.isNullOrEmpty()) {
                addProperty("cancelReason", reason)
            }
        }.toString()
        socketService.sendMessage(activeConversationId, jsonPayload, type = "SCHEDULE_INVITE")
    }

    fun performBookingAction(bookingId: String, action: String) {
        val newStatus = if (action == "ACCEPT") "ACCEPTED" else "REJECTED"
        updateBookingMessageStatus(bookingId, newStatus)
        notifyBookingStatusChanged(bookingId, newStatus)
        viewModelScope.launch {
            val result = if (action == "ACCEPT") {
                bookingRepository.acceptBooking(bookingId)
            } else {
                bookingRepository.rejectBooking(bookingId)
            }
            when (result) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Thao tác thành công"))
                }
                is Resource.Error -> {
                    _event.emit(UiEvent.ShowToast("Thao tác thất bại: ${result.message}"))
                }
            }
        }
    }

    fun sendScheduleInvite(request: ScheduleInviteRequest) {
        if (activeConversationId.isEmpty()) return
        viewModelScope.launch {
            when (val result = bookingRepository.scheduleInvite(activeConversationId, request)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã gửi lời mời lịch trình thành công"))
                    fetchMessages(activeConversationId)
                }
                is Resource.Error -> {
                    _event.emit(UiEvent.ShowToast("Gửi lời mời thất bại: ${result.message}"))
                }
            }
        }
    }
}
