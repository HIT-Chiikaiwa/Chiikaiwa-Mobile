package com.example.myapplication.ui.home.chat.group

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mapper.ChatMapper
import com.example.myapplication.data.model.Conversation
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class GroupViewModel(application: Application) : BaseViewModel<Conversation>(application) {

    private val repository = GroupRepository(application)

    fun updateGroupInfo(conversationId: String, groupName: String?, groupAvatar: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.updateGroup(conversationId, groupName, groupAvatar)) {
                is Resource.Success -> {
                    val updatedConv = ChatMapper.toDomain(result.data.data)
                    _uiState.value = UiState.Success(updatedConv)
                    _event.emit(UiEvent.ShowToast("Cập nhật thông tin nhóm thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun addMembers(conversationId: String, memberIds: List<String>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.addMembers(conversationId, memberIds)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã thêm thành viên vào nhóm"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun removeMember(conversationId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.removeMember(conversationId, userId)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã xóa thành viên khỏi nhóm"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun dissolveGroup(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.dissolveGroup(conversationId)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã giải tán nhóm"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun transferOwnership(conversationId: String, newOwnerId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.transferOwnership(conversationId, newOwnerId)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã chuyển quyền trưởng nhóm"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
