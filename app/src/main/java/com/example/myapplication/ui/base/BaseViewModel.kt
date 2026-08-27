package com.example.myapplication.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class BaseViewModel<T>(application: Application) : AndroidViewModel(application){

    protected val _uiState = MutableStateFlow<UiState<T>>(UiState.Idle)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

    protected val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()
}