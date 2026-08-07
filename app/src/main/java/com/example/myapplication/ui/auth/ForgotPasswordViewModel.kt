package com.example.myapplication.ui.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(application: Application) : BaseViewModel<String>(application) {
    private val repository = AuthRepository(application)

    fun resetPassword(email: String, pass: String, confirmPass: String) {
        if (pass.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập mật khẩu mới")
            return
        }

        val passwordRegex = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$".toRegex()
        if (!passwordRegex.matches(pass)) {
            _uiState.value = UiState.Error("Mật khẩu phải bao gồm cả chữ, số và ký tự đặc biệt (tối thiểu 8 ký tự)")
            return
        }

        if (confirmPass.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập lại mật khẩu mới")
            return
        }

        if (pass != confirmPass) {
            _uiState.value = UiState.Error("Mật khẩu xác nhận không khớp")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.resetPassword(email, pass, confirmPass)) {
                is Resource.Success -> {
                    val msg = result.data?.message ?: result.data?.data?.message ?: "Tạo mật khẩu mới thành công!"
                    _uiState.value = UiState.Success(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
                is Resource.Error -> {
                    val errorMessage = result.message ?: "Đặt lại mật khẩu thất bại"
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }
}
