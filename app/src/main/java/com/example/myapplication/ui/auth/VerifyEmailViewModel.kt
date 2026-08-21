package com.example.myapplication.ui.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.auth.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class VerifyEmailViewModel(application: Application) : BaseViewModel<String>(application) {

    private val repository = AuthRepository(application)

    fun sendOtp(email: String, flow: String) {
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
                    val msg = result.data?.message ?: result.data?.data?.message ?: "Gửi mã OTP thành công"
                    _uiState.value = UiState.Success(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
                is Resource.Error -> {
                    val errorMessage = result.message ?: "Gửi mã OTP thất bại"
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
