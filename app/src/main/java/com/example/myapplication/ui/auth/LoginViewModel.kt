package com.example.myapplication.ui.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : BaseViewModel<Unit>(application) {

    private val repository = AuthRepository(application)

    private val preferenceManager = PreferenceManager(application)

    fun login(email: String, password: String) {

        if (email.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập email")
            return
        }

        if (password.isBlank()) {
            _uiState.value = UiState.Error("Vui lòng nhập mật khẩu")
            return
        }

        viewModelScope.launch {

            _uiState.value = UiState.Loading

            when (val result = repository.login(email, password)) {

                is Resource.Success -> {

                    result.data?.let { loginResponse ->
                        val loginData = loginResponse.data

                        preferenceManager.saveLogin(
                            accessToken = loginData.accessToken,
                            refreshToken = loginData.refreshToken,
                            userId = loginData.id
                        )

                        _uiState.value = UiState.Success(Unit)

                        _event.value = UiEvent.ShowToast("Đăng nhập thành công")
                        _event.value = UiEvent.NavigateHome
                    }
                }

                is Resource.Error -> {
                    android.util.Log.d("LOGIN_ERROR", "Message: ${result.message}")

                    val errorMessage = result.message ?: "Đăng nhập thất bại, vui lòng thử lại"
                    _uiState.value = UiState.Error(errorMessage)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}