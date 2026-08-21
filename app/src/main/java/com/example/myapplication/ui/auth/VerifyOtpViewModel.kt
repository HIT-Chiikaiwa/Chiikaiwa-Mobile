package com.example.myapplication.ui.auth

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.auth.AuthRepository
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
                    val msg = result.data?.message ?: result.data?.data?.message ?: "Xác thực OTP thành công"
                    _uiState.value = UiState.Success(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }

                is Resource.Error -> {
                    val errorMessage = result.message ?: "Xác thực OTP thất bại"
                    _uiState.value = UiState.Error(errorMessage)
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
                    val msg = result.data?.message ?: result.data?.data?.message ?: "Đã gửi lại mã OTP"
                    _uiState.value = UiState.Idle
                    _event.emit(UiEvent.ShowToast(msg))
                    startResendTimer()
                }

                is Resource.Error -> {
                    val errorMessage = result.message ?: "Gửi lại mã OTP thất bại"
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}