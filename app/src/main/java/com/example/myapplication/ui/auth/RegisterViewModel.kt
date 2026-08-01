package com.example.myapplication.ui.auth

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.request.RegisterRequest
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : BaseViewModel<String>(application) {

    private val repository = AuthRepository(application)

    fun register(
        lastName: String,
        firstName: String,
        gender: String,
        dateOfBirth: String,
        email: String,
        pass: String,
        confirmPass: String
    ) {

        if (lastName.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập họ")
            return
        }

        if (firstName.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập tên")
            return
        }

        if (gender.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng chọn giới tính")
            return
        }

        if (dateOfBirth.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng chọn ngày sinh")
            return
        }

        if (email.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập email")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = UiState.Error("Email không đúng định dạng")
            return
        }

        if (pass.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập mật khẩu")
            return
        }

        if (pass.length < 8) {
            _uiState.value = UiState.Error("Mật khẩu phải có ít nhất 8 ký tự")
            return
        }

        if (confirmPass.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập lại mật khẩu")
            return
        }

        if (pass != confirmPass) {
            _uiState.value = UiState.Error("Mật khẩu xác nhận không khớp")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val request = RegisterRequest(
                email = email,
                password = pass,
                confirmPassword = confirmPass,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                dateOfBirth = dateOfBirth
            )

            when (val result = repository.register(request)) {

                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data?.data?.message ?: "Đăng ký thành công")
                    viewModelScope.launch { _event.emit(UiEvent.ShowToast(result.data?.data?.message ?: "Đăng ký thành công")) }
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