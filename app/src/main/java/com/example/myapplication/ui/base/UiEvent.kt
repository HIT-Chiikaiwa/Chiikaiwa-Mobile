package com.example.myapplication.ui.base

sealed class UiEvent {

    data class ShowToast(val message: String) : UiEvent()

    data object NavigateHome : UiEvent()

}