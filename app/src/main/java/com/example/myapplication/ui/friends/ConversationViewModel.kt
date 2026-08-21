package com.example.myapplication.ui.friends

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.remote.websocket.WebSocketManager
import com.example.myapplication.data.repository.social.BlockRepository
import com.example.myapplication.data.repository.chat.ConversationRepository
import com.example.myapplication.data.repository.chat.MessageRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class ConversationViewModel(application: Application) : BaseViewModel<List<ConversationResponse>>(application) {

    private val conversationRepository = ConversationRepository(application)
    private val blockRepository = BlockRepository(application)
    private val preferenceManager = PreferenceManager(application)
    private val socketService = WebSocketManager

    private val partnerIdCache = mutableMapOf<String, String>()

    init {
        initWebSocket()
        fetchConversations()
    }

    private fun initWebSocket() {
        val currentUserId = preferenceManager.getUserId() ?: ""
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch {
                socketService.messageFlow.collect {
                    fetchConversations()
                }
            }
        }
    }

    fun fetchConversations() {
        viewModelScope.launch {
            val blockedResult = blockRepository.getBlockedUsers()
            val blockedIds = if (blockedResult is Resource.Success) {
                blockedResult.data?.data?.mapNotNull { it.id }?.toSet() ?: emptySet()
            } else {
                emptySet()
            }

            val currentUserId = preferenceManager.getUserId() ?: ""

            when (val result = conversationRepository.getConversations()) {
                is Resource.Success -> {
                    val rawList = result.data?.data?.content ?: emptyList()
                    val filteredList = mutableListOf<ConversationResponse>()

                    for (conv in rawList) {
                        val lastSenderId = conv.lastMessage?.senderId

                        if (!conv.groupName.isNullOrEmpty()) {
                            filteredList.add(conv)
                            continue
                        }

                        if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId && blockedIds.contains(lastSenderId)) {
                            continue
                        }

                        val cachedPartnerId = partnerIdCache[conv.id]
                        if (cachedPartnerId != null) {
                            if (blockedIds.contains(cachedPartnerId)) {
                                continue
                            }
                        } else {
                            resolvePartnerIdAndFilter(conv, currentUserId, blockedIds)
                        }

                        filteredList.add(conv)
                    }
                    _uiState.value = UiState.Success(filteredList)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private fun resolvePartnerIdAndFilter(
        conv: ConversationResponse,
        currentUserId: String,
        blockedIds: Set<String>
    ) {
        val lastSenderId = conv.lastMessage?.senderId
        if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId) {
            partnerIdCache[conv.id] = lastSenderId
            return
        }

        viewModelScope.launch {
            val messageRepository = MessageRepository(getApplication())
            val result = messageRepository.getMessages(conv.id, page = 0, size = 20)
            if (result is Resource.Success) {
                val messages = result.data.data.content
                val partnerId = messages.firstOrNull { it.senderId != currentUserId }?.senderId
                if (!partnerId.isNullOrEmpty()) {
                    partnerIdCache[conv.id] = partnerId
                    if (blockedIds.contains(partnerId)) {
                        // Re-fetch to apply updated filter, but guard against infinite recursion
                        fetchConversations()
                    }
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
}
