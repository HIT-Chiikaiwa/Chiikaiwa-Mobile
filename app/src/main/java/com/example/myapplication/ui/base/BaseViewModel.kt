package com.example.myapplication.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.utils.common.SingleLiveEvent

open class BaseViewModel<T>(application: Application) : AndroidViewModel(application){

    protected val _uiState = MutableLiveData<UiState<T>>(UiState.Idle)
    val uiState: LiveData<UiState<T>> = _uiState

    protected val _event = SingleLiveEvent<UiEvent>()
    val event: LiveData<UiEvent> = _event
}