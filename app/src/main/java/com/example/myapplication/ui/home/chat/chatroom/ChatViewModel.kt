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
import com.example.myapplication.data.repository.ProfileRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.BookingMessageHelper
import com.example.myapplication.utils.resource.Resource
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    private val profileRepository = ProfileRepository(application)
    private val gson = Gson()

    val currentUserId: String = preferenceManager.getUserId() ?: ""
    private var activeConversationId: String = ""
    private var currentTargetUserId: String = ""
    private val _messages = mutableListOf<Message>()
    private var currentUserAvatar: String? = null
    private var partnerAvatar: String? = null

    companion object {
        private const val TAG = "ChatViewModel"
        private const val TEMP_PREFIX = "temp_"
        private const val TEMP_MSG_TTL = 15_000L
        private const val IMAGE_UPLOAD_WAIT = 1_500L
    }

    init {
        fetchCurrentUserAvatar()
        initWebSocket()
    }

    private fun fetchCurrentUserAvatar() {
        viewModelScope.launch {
            val res = profileRepository.getCurrentUser()
            if (res is Resource.Success) {
                currentUserAvatar = res.data.data.avatar
                updateState()
            }
        }
    }

    private fun fillMissingAvatars() {
        for (i in _messages.indices) {
            val msg = _messages[i]
            val avatar = msg.sender.avatar
            if (!avatar.isNullOrEmpty()) continue

            val targetAvatar = if (msg.sender.id == currentUserId) currentUserAvatar else partnerAvatar
            if (!targetAvatar.isNullOrEmpty()) {
                _messages[i] = msg.copy(sender = msg.sender.copy(avatar = targetAvatar))
            }
        }
    }

    private fun updateState() {
        fillMissingAvatars()
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
            val rawJsonObj = gson.fromJson(body, JsonObject::class.java)
            val dataObj = if (rawJsonObj.has("data") && rawJsonObj.get("data")?.isJsonObject == true) {
                rawJsonObj.getAsJsonObject("data")
            } else rawJsonObj

            if (handleBookingStatusUpdate(dataObj)) return

            val incomingMsg = messageParser.parseWsMessage(body, activeConversationId) ?: run {
                updateState()
                return
            }

            val finalMsg = if (incomingMsg.sender.id.isEmpty() || incomingMsg.sender.id == currentUserId) {
                incomingMsg.copy(sender = User(id = currentUserId, fullName = "Tôi", avatar = currentUserAvatar))
            } else incomingMsg

            handleSystemMessage(finalMsg)
            addOrReplaceMessage(finalMsg)
            updateState()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WS message", e)
        }
    }

    private fun handleBookingStatusUpdate(dataObj: JsonObject): Boolean {
        val contentStr = dataObj.get("content")?.takeIf { !it.isJsonNull }?.asString ?: ""
        var bookingId = dataObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
        var status = dataObj.get("status")?.takeIf { !it.isJsonNull }?.asString
        var reason = dataObj.get("cancelReason")?.takeIf { !it.isJsonNull }?.asString

        if (bookingId.isNullOrEmpty() && contentStr.isNotEmpty()) {
            try {
                val contentJson = gson.fromJson(contentStr, JsonObject::class.java)
                bookingId = contentJson?.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
                if (status.isNullOrEmpty()) status = contentJson?.get("status")?.takeIf { !it.isJsonNull }?.asString
                if (reason.isNullOrEmpty()) reason = contentJson?.get("cancelReason")?.takeIf { !it.isJsonNull }?.asString
            } catch (_: Exception) {}
        }

        if (bookingId.isNullOrEmpty() || status.isNullOrEmpty()) return false

        bookingManager.updateBookingMessageStatus(_messages, bookingId, status, reason)

        val isStatusOnly = !contentStr.contains("scheduledAt") && !dataObj.has("scheduledAt")
        if (isStatusOnly) {
            updateState()
            return true
        }
        return false
    }

    private fun handleSystemMessage(msg: Message) {
        if (msg.type == MessageType.SYSTEM) {
            bookingManager.processIncomingSystemMessage(_messages, msg)
        }
    }

    private fun addOrReplaceMessage(msg: Message) {
        if (msg.type == MessageType.SYSTEM) {
            val isBookingNotification = BookingMessageHelper.extractStatusFromSystemMessage(msg.content) != null
            if (isBookingNotification) return
        }

        if (msg.type == MessageType.IMAGE) {
            val cleanUrl = msg.content.trimEnd(',', ';', ' ', '"', '\'')
            val cleanMsg = msg.copy(content = cleanUrl)
            val existingIdx = _messages.indexOfFirst {
                it.id == cleanMsg.id ||
                it.content.trimEnd(',', ';', ' ', '"', '\'') == cleanUrl
            }
            if (existingIdx != -1) {
                _messages[existingIdx] = cleanMsg
                val tempIdx = _messages.indexOfFirst { it.id.startsWith(TEMP_PREFIX) && it.type == MessageType.IMAGE }
                if (tempIdx != -1) {
                    _messages.removeAt(tempIdx)
                }
            } else {
                val tempIdx = _messages.indexOfFirst { it.id.startsWith(TEMP_PREFIX) && it.type == MessageType.IMAGE }
                if (tempIdx != -1) {
                    _messages[tempIdx] = cleanMsg
                } else {
                    _messages.add(cleanMsg)
                }
            }
        } else {
            _messages.removeAll {
                it.id == msg.id ||
                (it.id.startsWith(TEMP_PREFIX) && (it.type == msg.type || it.content == msg.content))
            }
            if (_messages.none { it.id == msg.id }) {
                _messages.add(msg)
            }
        }
    }

    fun initChatSession(convId: String, targetId: String) {
        if (targetId.isNotEmpty()) {
            currentTargetUserId = targetId
            viewModelScope.launch {
                val res = profileRepository.getProfile(targetId)
                if (res is Resource.Success) {
                    partnerAvatar = res.data.data.avatar
                    updateState()
                }
            }
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

        val tempMsg = createTempMessage(msgText, MessageType.TEXT)
        _messages.add(tempMsg)
        updateState()
        socketService.sendMessage(activeConversationId, msgText)
    }

    fun sendImageMessage(file: MultipartBody.Part, localImagePath: String = "") {
        if (activeConversationId.isEmpty()) {
            if (currentTargetUserId.isNotEmpty()) {
                viewModelScope.launch {
                    when (val result = conversationRepository.createOrGetDirectConversation(currentTargetUserId)) {
                        is Resource.Success -> {
                            activeConversationId = result.data.data.id
                            socketService.subscribeToChat(currentUserId, activeConversationId)
                            sendImageMessage(file, localImagePath)
                        }
                        is Resource.Error -> {
                            _event.emit(UiEvent.ShowToast("Gửi ảnh thất bại: ${result.message}"))
                        }
                    }
                }
            } else {
                viewModelScope.launch {
                    _event.emit(UiEvent.ShowToast("Gửi ảnh thất bại: Chưa xác định cuộc hội thoại"))
                }
            }
            return
        }

        val tempMsg = createTempMessage(localImagePath, MessageType.IMAGE)
        _messages.add(tempMsg)
        updateState()

        viewModelScope.launch {
            when (val result = messageRepository.uploadImage(activeConversationId, file)) {
                is Resource.Success -> {
                    delay(IMAGE_UPLOAD_WAIT)
                    if (_messages.any { it.id == tempMsg.id }) {
                        fetchMessages(activeConversationId)
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

    private fun createTempMessage(content: String, type: MessageType) = Message(
        id = "${TEMP_PREFIX}${System.currentTimeMillis()}",
        conversationId = activeConversationId,
        sender = User(id = currentUserId, fullName = "Tôi", avatar = currentUserAvatar),
        content = content,
        type = type,
        status = MessageStatus.SENT,
        createdAt = "Vừa xong",
        updatedAt = "",
        isRecalled = false
    )

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

    fun clearBookingOverride(bookingId: String) {
        bookingManager.clearBookingOverride(bookingId)
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
                is Resource.Success -> _event.emit(UiEvent.ShowToast("Thao tác thành công"))
                is Resource.Error -> _event.emit(UiEvent.ShowToast("Thao tác thất bại: ${result.message}"))
            }
        }
    }

    fun notifyBookingStatusChanged(bookingId: String, newStatus: String, reason: String? = null) {
        if (activeConversationId.isEmpty()) return
        val payload = JsonObject().apply {
            addProperty("bookingId", bookingId)
            addProperty("status", newStatus)
            if (!reason.isNullOrEmpty()) addProperty("cancelReason", reason)
        }.toString()
        socketService.sendMessage(activeConversationId, payload, type = "SCHEDULE_INVITE")
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

    fun fetchMessages(conversationId: String = activeConversationId, page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            when (val result = messageRepository.getMessages(conversationId, page, size)) {
                is Resource.Success -> {
                    try {
                        val rawList = result.data.data.content.map { ChatMapper.toDomain(it) }
                        val processedList = bookingManager.processMessageList(rawList)

                        cacheAvatarsFromMessages(processedList)
                        mergeServerMessages(processedList)
                        syncBookingStatuses(processedList)
                        updateState()

                        if (conversationId.isNotEmpty()) socketService.sendReadReceipt(conversationId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing messages", e)
                        updateState()
                    }
                }
                is Resource.Error -> updateState()
            }
        }
    }

    private fun cacheAvatarsFromMessages(messages: List<Message>) {
        messages.firstOrNull { it.sender.id != currentUserId && !it.sender.avatar.isNullOrEmpty() }
            ?.sender?.avatar?.let { partnerAvatar = it }
        messages.firstOrNull { it.sender.id == currentUserId && !it.sender.avatar.isNullOrEmpty() }
            ?.sender?.avatar?.let { currentUserAvatar = it }
    }

    private fun mergeServerMessages(serverMessages: List<Message>) {
        val now = System.currentTimeMillis()
        val recentTemps = _messages.filter { msg ->
            if (!msg.id.startsWith(TEMP_PREFIX)) return@filter false
            val tempTime = msg.id.substringAfter(TEMP_PREFIX).toLongOrNull() ?: 0L
            (now - tempTime) < TEMP_MSG_TTL
        }.toMutableList()

        _messages.clear()
        _messages.addAll(serverMessages.reversed())

        // Deduplicate recent temporary messages against newly loaded server messages
        val realMyImages = _messages.filter { it.type == MessageType.IMAGE && !it.id.startsWith(TEMP_PREFIX) && it.sender.id == currentUserId }
        val tempImages = recentTemps.filter { it.type == MessageType.IMAGE }
        
        var matchedImageCount = 0
        val tempImagesToRemove = mutableListOf<Message>()
        for (tempImg in tempImages) {
            if (matchedImageCount < realMyImages.size) {
                tempImagesToRemove.add(tempImg)
                matchedImageCount++
            }
        }
        recentTemps.removeAll(tempImagesToRemove)

        val tempTextsToRemove = mutableListOf<Message>()
        val tempTexts = recentTemps.filter { it.type == MessageType.TEXT }
        for (tempText in tempTexts) {
            val hasMatchingRealText = _messages.any {
                it.type == MessageType.TEXT &&
                !it.id.startsWith(TEMP_PREFIX) &&
                it.sender.id == currentUserId &&
                it.content == tempText.content
            }
            if (hasMatchingRealText) {
                tempTextsToRemove.add(tempText)
            }
        }
        recentTemps.removeAll(tempTextsToRemove)

        recentTemps.forEach { temp ->
            if (_messages.none { it.id == temp.id }) {
                _messages.add(temp)
            }
        }
    }

    private suspend fun syncBookingStatuses(messages: List<Message>) {
        val bookingIds = messages.mapNotNull { msg ->
            if (msg.type != MessageType.BOOKING && !BookingMessageHelper.isBookingMessage(msg.content)) return@mapNotNull null
            try {
                gson.fromJson(msg.content, JsonObject::class.java)
                    ?.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
            } catch (_: Exception) { null }
        }.toSet()

        if (bookingIds.isEmpty()) return

        var updatedAny = false
        coroutineScope {
            bookingIds.map { bId ->
                async {
                    val result = bookingRepository.getBookingDetail(bId)
                    if (result is Resource.Success) {
                        val booking = result.data.data
                        if (!booking.status.isNullOrEmpty()) {
                            if (bookingManager.updateBookingMessageStatus(_messages, bId, booking.status, booking.cancelReason)) {
                                updatedAny = true
                            }
                        }
                        if (booking.hasRated == true && booking.myRating != null) {
                            if (bookingManager.updateBookingMessageRating(_messages, bId, booking.myRating!!)) {
                                updatedAny = true
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        if (updatedAny) updateState()
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
}
