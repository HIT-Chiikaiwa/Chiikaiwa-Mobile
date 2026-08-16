package com.example.myapplication.ui.home.chat

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.BlockedUserDto
import com.example.myapplication.data.repository.BlockRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class BlockUserViewModel(application: Application) : BaseViewModel<Unit>(application) {

    private val blockRepository = BlockRepository(getApplication())

    fun blockUser(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = blockRepository.blockUser(userId)) {
                is Resource.Success -> {
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun getBlockedUsers(onResult: (List<BlockedUserDto>) -> Unit) {
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
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
