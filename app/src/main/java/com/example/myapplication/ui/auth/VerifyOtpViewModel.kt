package com.example.myapplication.ui.auth

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VerifyOtpViewModel(application: Application) : BaseViewModel<String>(application) {

    private val repository = AuthRepository(application)

    private val _resendCooldown = MutableStateFlow<Int>(0)
    val resendCooldown: StateFlow<Int> get() = _resendCooldown

    private var timerJob: Job? = null

    fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in 60 downTo 0) {
                _resendCooldown.value = i
                delay(1000)
            }
        }
    }

    fun verifyOtp(email: String, otpCode: String, flow: String) {
        if (otpCode.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập mã OTP")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val apiCall = if (flow == "forgot_password") {
                repository.forgotPasswordVerifyOtp(email, otpCode)
            } else {
                repository.verifyRegisterOtp(email, otpCode)
            }

            when (val result = apiCall) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data?.data?.message ?: "Xác thực OTP thành công")
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast(result.data?.data?.message ?: "Xác thực OTP thành công")) }
                }

                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun resendOtp(email: String, flow: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Không tìm thấy email")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val apiCall = if (flow == "forgot_password") {
                repository.forgotPasswordSendOtp(email)
            } else {
                repository.sendOtp(email)
            }

            when (val result = apiCall) {
                is Resource.Success -> {
                    _uiState.value = UiState.Idle
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast(result.data?.data?.message ?: "Đã gửi lại mã OTP")) }
                    startResendTimer()
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