package com.example.myapplication.ui.home.schedule

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.request.CancelBookingRequest
import com.example.myapplication.data.remote.dto.request.CreateBookingRequest
import com.example.myapplication.data.remote.dto.request.RateBookingRequest
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.data.remote.dto.response.WeeklyBookingResponse
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookingViewModel(application: Application) : BaseViewModel<BookingDto>(application) {

    private val repository = BookingRepository(application)

    private val _myBookings = MutableStateFlow<List<BookingDto>>(emptyList())
    val myBookings: StateFlow<List<BookingDto>> get() = _myBookings

    private val _weeklyBookings = MutableStateFlow<WeeklyBookingResponse?>(null)
    val weeklyBookings: StateFlow<WeeklyBookingResponse?> get() = _weeklyBookings

    fun createBooking(conversationId: String, request: CreateBookingRequest) {
        if (conversationId.isBlank()) {
            viewModelScope.launch { _event.emit(UiEvent.ShowToast("Không tìm thấy cuộc trò chuyện")) }
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.createBookingInConversation(conversationId, request)) {
                is Resource.Success -> {
                    val booking = result.data.data
                    _uiState.value = UiState.Success(booking)
                    _event.emit(UiEvent.ShowToast("Tạo cuộc hẹn thành công!"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                    _event.emit(UiEvent.ShowToast("Tạo cuộc hẹn thất bại: ${result.message}"))
                }
            }
        }
    }

    fun loadMyBookings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getMyBookings()) {
                is Resource.Success -> {
                    val list = result.data.data ?: emptyList()
                    _myBookings.value = list
                    _uiState.value = UiState.Idle
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun loadWeeklyBookings(weekStart: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getWeeklyBookings(weekStart)) {
                is Resource.Success -> {
                    _weeklyBookings.value = result.data.data
                    _uiState.value = UiState.Idle
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.acceptBooking(bookingId)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Đã chấp nhận cuộc hẹn"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun rejectBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.rejectBooking(bookingId)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Đã từ chối cuộc hẹn"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun cancelBooking(bookingId: String, cancelReason: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.cancelBooking(bookingId, cancelReason)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                    _event.emit(UiEvent.ShowToast("Đã hủy cuộc hẹn"))
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
