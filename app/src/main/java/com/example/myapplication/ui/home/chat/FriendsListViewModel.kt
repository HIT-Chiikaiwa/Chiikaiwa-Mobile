package com.example.myapplication.ui.home.chat

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.repository.ConversationRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class FriendsListViewModel(application: Application) : BaseViewModel<List<ConversationResponse>>(application) {

    private val conversationRepository = ConversationRepository(application)

    fun fetchConversations() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
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

    fun searchConversations(keyword: String) {
        if (keyword.isBlank()) {
            fetchConversations()
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
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
