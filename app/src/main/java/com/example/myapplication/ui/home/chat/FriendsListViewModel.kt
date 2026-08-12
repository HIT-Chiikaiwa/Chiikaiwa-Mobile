package com.example.myapplication.ui.home.chat

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.remote.network.NetworkConstants
import com.example.myapplication.data.remote.websocket.WebSocketManager
import com.example.myapplication.data.repository.ConversationRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class FriendsListViewModel(application: Application) : BaseViewModel<List<ConversationResponse>>(application) {

    private val conversationRepository = ConversationRepository(application)
    private val preferenceManager = PreferenceManager(application)
    private val partnerIdCache = mutableMapOf<String, String>()

    private val socketService = WebSocketManager

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

                        // 1. If group chat, do not filter based on blocked user
                        if (!conv.groupName.isNullOrEmpty()) {
                            filteredList.add(conv)
                            continue
                        }

                        // 2. If direct chat and the last sender was someone else and they are blocked, filter them out
                        if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId && blockedIds.contains(lastSenderId)) {
                            continue
                        }

                        // 3. If direct chat and the last sender was current user, check cached partner id
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
            val messageRepository = com.example.myapplication.data.repository.MessageRepository(getApplication())
            val result = messageRepository.getMessages(conv.id, page = 0, size = 20)
            if (result is Resource.Success) {
                val messages = result.data.data.content
                val partnerId = messages.firstOrNull { it.senderId != currentUserId }?.senderId
                if (!partnerId.isNullOrEmpty()) {
                    partnerIdCache[conv.id] = partnerId
                    if (blockedIds.contains(partnerId)) {
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

    fun getPendingFriendRequests(onResult: (List<com.example.myapplication.data.remote.dto.response.FriendDto>) -> Unit) {
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
                    fetchConversations()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    private val blockRepository = com.example.myapplication.data.repository.BlockRepository(getApplication())

    fun blockUser(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = blockRepository.blockUser(userId)) {
                is Resource.Success -> {
                    onSuccess()
                    fetchConversations()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun getBlockedUsers(onResult: (List<com.example.myapplication.data.remote.dto.response.BlockedUserDto>) -> Unit) {
        viewModelScope.launch {
            when (val result = blockRepository.getBlockedUsers()) {
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

    fun unblockUser(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = blockRepository.unblockUser(userId)) {
                is Resource.Success -> {
                    onSuccess()
                    fetchConversations()
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
