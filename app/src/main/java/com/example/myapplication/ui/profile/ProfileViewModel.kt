package com.example.myapplication.ui.profile

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.dto.response.SubjectDto
import com.example.myapplication.data.repository.profile.ProfileRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : BaseViewModel<UserDto>(application) {

    private val repository = ProfileRepository(application)
    private val preferenceManager = PreferenceManager(application)

    private val bookingRepository = com.example.myapplication.data.repository.schedule.BookingRepository(application)
    private val _subjects = MutableStateFlow<List<SubjectDto>>(emptyList())
    val subjects: StateFlow<List<SubjectDto>> get() = _subjects

    private val _appointmentCount = MutableStateFlow<Int>(0)
    val appointmentCount: StateFlow<Int> get() = _appointmentCount

    fun getUserId(): String? = preferenceManager.getUserId()

    fun loadProfile(targetUserId: String? = null) {
        val currentUserId = getUserId()
        val userId = targetUserId ?: currentUserId ?: return
        val isSelf = targetUserId == null || targetUserId == currentUserId
        if (isSelf) {
            loadAppointmentCount()
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (isSelf) {
                val currentUserRes = repository.getCurrentUser()
                var currentUserDto: UserDto? = null
                if (currentUserRes is Resource.Success) {
                    currentUserDto = currentUserRes.data.data
                    currentUserDto?.email?.let { email ->
                        if (email.isNotEmpty()) preferenceManager.saveEmail(email)
                    }
                }

                when (val profileRes = repository.getProfile(userId)) {
                    is Resource.Success -> {
                        val profileUser = profileRes.data.data
                        val mergedUser = if (currentUserDto != null) {
                            profileUser.copy(
                                email = currentUserDto.email ?: profileUser.email
                            )
                        } else {
                            profileUser
                        }
                        _uiState.value = UiState.Success(mergedUser)
                        _subjects.value = mergedUser.subjects ?: emptyList()
                    }
                    is Resource.Error -> {
                        if (currentUserDto != null) {
                            _uiState.value = UiState.Success(currentUserDto)
                        } else {
                            _uiState.value = UiState.Error(profileRes.message)
                        }
                    }
                }
            } else {
                when (val profileRes = repository.getProfile(userId)) {
                    is Resource.Success -> {
                        val profileUser = profileRes.data.data
                        _uiState.value = UiState.Success(profileUser)
                        _subjects.value = profileUser.subjects ?: emptyList()
                    }
                    is Resource.Error -> {
                        _uiState.value = UiState.Error(profileRes.message)
                    }
                }
            }
        }
    }

    fun loadAppointmentCount() {
        viewModelScope.launch {
            when (val result = bookingRepository.getMyBookings()) {
                is Resource.Success -> {
                    val list = result.data.data ?: emptyList()
                    val validCount = list.count { booking ->
                        val status = booking.status?.uppercase(java.util.Locale.getDefault())
                        status != "CANCELLED" && status != "REJECTED" && status != "EXPIRED"
                    }
                    _appointmentCount.value = validCount
                }
                is Resource.Error -> {
                    _appointmentCount.value = 0
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

    private val authRepository = com.example.myapplication.data.repository.auth.AuthRepository(application)

    fun logout() {
        com.example.myapplication.data.remote.websocket.WebSocketManager.disconnect()
        val refreshToken = preferenceManager.getRefreshToken()
        viewModelScope.launch {
            if (!refreshToken.isNullOrEmpty()) {
                authRepository.logout(refreshToken)
            }
            preferenceManager.logout()
            _event.emit(UiEvent.NavigateHome)
        }
    }

    fun updateStatusTag(statusTag: String) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.updateStatusTag(userId, UpdateStatusTagRequest(statusTag))) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Cập nhật trạng thái thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun updatePersonalInfo(request: UpdatePersonalInfoRequest) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.updatePersonalInfo(userId, request)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Cập nhật thông tin cá nhân thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun updateAcademicInfo(request: UpdateAcademicInfoRequest) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.updateAcademicInfo(userId, request)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Cập nhật thông tin học vấn thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun updateProfileLocation(location: String) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.updateProfileLocation(userId, UpdateProfileLocationRequest(location))) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Cập nhật địa điểm thành công"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast("Lỗi cập nhật địa điểm: ${result.message}"))
                }
            }
        }
    }

    fun updateFullProfileInfo(personalRequest: UpdatePersonalInfoRequest, academicRequest: UpdateAcademicInfoRequest, location: String) {
        val userId = getUserId() ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val personalResult = repository.updatePersonalInfo(userId, personalRequest)
            if (personalResult is Resource.Error) {
                _uiState.value = UiState.Error(personalResult.message)
                _event.emit(UiEvent.ShowToast("Lỗi cập nhật thông tin cá nhân: ${personalResult.message}"))
                return@launch
            }

            val academicResult = repository.updateAcademicInfo(userId, academicRequest)
            if (academicResult is Resource.Error) {
                _uiState.value = UiState.Error(academicResult.message)
                _event.emit(UiEvent.ShowToast("Lỗi cập nhật học vấn: ${academicResult.message}"))
                return@launch
            }

            val locationResult = repository.updateProfileLocation(userId, UpdateProfileLocationRequest(location))
            if (locationResult is Resource.Error) {
                _uiState.value = UiState.Error(locationResult.message)
                _event.emit(UiEvent.ShowToast("Lỗi cập nhật địa điểm: ${locationResult.message}"))
                return@launch
            }

            if (locationResult is Resource.Success) {
                _uiState.value = UiState.Success(locationResult.data.data)
                _event.emit(UiEvent.ShowToast("Cập nhật thông tin thành công"))
            }
        }
    }
}
