package com.example.myapplication.data.repository

import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.remote.ApiService

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(emailOrPhone: String, password: String) = apiService.login(
        LoginRequest(emailOrPhone = emailOrPhone, password = password)
    )

    suspend fun getCurrentUser() = apiService.getCurrentUser()
}