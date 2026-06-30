package com.example.myapplication.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.response.LoginResponse
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository(RetrofitClient.apiService)

    private val _loginResult = MutableLiveData<LoginResponse?>()

    val loginResult: LiveData<LoginResponse?> = _loginResult

    private val _error = MutableLiveData<String>()

    val error: LiveData<String> = _error

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)

                if (response.isSuccessful) {
                    _loginResult.value = response.body()
                } else {
                    _error.value = "Email hoặc mật khẩu không đúng"
                }
            } catch (e: Exception) {
                _error.value = e.message?: "Không thể kết nối tới server"
            }
        }

    }

}