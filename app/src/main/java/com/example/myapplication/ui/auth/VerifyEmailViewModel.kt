package com.example.myapplication.ui.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.Resource
import kotlinx.coroutines.launch

class VerifyEmailViewModel(application: Application) : BaseViewModel<String>(application) {

    private val repository = AuthRepository(application)

    fun sendOtp(email: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Không tìm thấy email")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = repository.sendOtp(email)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data?.message ?: "Gửi mã OTP thành công")
                    _event.value = UiEvent.ShowToast(result.data?.message ?: "Gửi mã OTP thành công")
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
