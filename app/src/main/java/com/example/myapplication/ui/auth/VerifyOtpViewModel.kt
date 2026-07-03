package com.example.myapplication.ui.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.Resource
import kotlinx.coroutines.launch

class VerifyOtpViewModel(application: Application) : BaseViewModel<String>(application) {

    private val repository = AuthRepository(application)

    fun verifyOtp(email: String, otpCode: String) {
        if (otpCode.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập mã OTP")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.verifyRegisterOtp(email, otpCode)) {

                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data?.message ?: "Xác thực OTP thành công")
                    _event.value = UiEvent.ShowToast(result.data?.message ?: "Xác thực OTP thành công")
                }

                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun resendOtp(email: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Không tìm thấy email")
            return
        }

        viewModelScope.launch {

            _uiState.value = UiState.Loading

            when (val result = repository.sendOtp(email)) {

                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data?.message ?: "Đã gửi lại mã OTP")
                    _event.value = UiEvent.ShowToast(result.data?.message ?: "Đã gửi lại mã OTP")
                }

                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)

                }
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}