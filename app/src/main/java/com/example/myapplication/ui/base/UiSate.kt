package com.example.myapplication.ui.base

sealed class UiState<out T> {

    object Idle : UiState<Nothing>()

    object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
        val version: Long = System.currentTimeMillis()
    ) : UiState<T>()

    data class Error(
        val message: String,
        val version: Long = System.currentTimeMillis()
    ) : UiState<Nothing>()
}