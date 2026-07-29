package com.example.myapplication.ui.profile

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.dto.response.SubjectDto
import com.example.myapplication.data.repository.ProfileRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : BaseViewModel<UserDto>(application) {

    private val repository = ProfileRepository(application)
    private val preferenceManager = PreferenceManager(application)

    private val _subjects = MutableStateFlow<List<SubjectDto>>(emptyList())
    val subjects: StateFlow<List<SubjectDto>> get() = _subjects

    fun getUserId(): String? = preferenceManager.getUserId()

    fun loadProfile() {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getProfile(userId)) {
                is Resource.Success -> {
                    val user = result.data.data
                    _uiState.value = UiState.Success(user)
                    _subjects.value = user.subjects ?: emptyList()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun uploadAvatar(imageFile: java.io.File) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.uploadAvatar(userId, imageFile)) {
                is Resource.Success -> {
                    val updatedUser = result.data.data
                    _uiState.value = UiState.Success(updatedUser)
                    _event.emit(UiEvent.ShowToast("Cập nhật ảnh đại diện thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast("Lỗi cập nhật ảnh: ${result.message}"))
                }
            }
        }
    }

    fun toggleBuddyStatus(buddyActive: Boolean) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.toggleBuddyStatus(userId, buddyActive)) {
                is Resource.Success -> {
                    val user = result.data.data
                    _uiState.value = UiState.Success(user)
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Cập nhật trạng thái Buddy thành công")) }
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun deleteAccount() {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.deleteAccount(userId)) {
                is Resource.Success -> {
                    preferenceManager.logout()
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Xoá tài khoản thành công")) }
                    viewModelScope.launch { _event.emit(UiEvent.NavigateHome) }
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun loadSubjects() {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            when (val result = repository.getSubjects(userId)) {
                is Resource.Success -> {
                    _subjects.value = result.data.data ?: emptyList()
                }
                is Resource.Error -> {
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Không thể tải danh sách môn học: ${result.message}")) }
                }
            }
        }
    }

    fun addSubject(name: String, type: String) {
        val userId = getUserId() ?: return
        if (name.isBlank()) {
            viewModelScope.launch { _event.emit(UiEvent.ShowToast("Tên môn học không được để trống")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.addSubject(userId, AddSubjectRequest(name, type))) {
                is Resource.Success -> {
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Thêm môn học thành công")) }
                    loadSubjects()
                    loadProfile()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Idle
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Lỗi: ${result.message}")) }
                }
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.deleteSubject(userId, subjectId)) {
                is Resource.Success -> {
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Xoá môn học thành công")) }
                    loadSubjects()
                    loadProfile()
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Idle
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Lỗi: ${result.message}")) }
                }
            }
        }
    }

    fun changePassword(request: ChangePasswordRequest) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.changePassword(userId, request)) {
                is Resource.Success -> {
                    val currentUser = (_uiState.value as? UiState.Success)?.data
                    if (currentUser != null) {
                        _uiState.value = UiState.Success(currentUser)
                    } else {
                        _uiState.value = UiState.Idle
                    }
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast("Đổi mật khẩu thành công")) }
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error("Đổi mật khẩu thất bại: ${result.message}")
                }
            }
        }
    }

    fun logout() {
        preferenceManager.logout()
        viewModelScope.launch { _event.emit(UiEvent.NavigateHome) }
    }
}
