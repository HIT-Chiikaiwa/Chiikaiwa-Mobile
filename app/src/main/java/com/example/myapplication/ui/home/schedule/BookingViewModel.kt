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

    private fun updateLocalBooking(updatedBooking: BookingDto) {
        val bookingId = updatedBooking.id
        if (bookingId.isNullOrEmpty()) return

        val currentList = _myBookings.value.toMutableList()
        val index = currentList.indexOfFirst { !it.id.isNullOrEmpty() && it.id == bookingId }
        if (index != -1) {
            currentList[index] = updatedBooking
            _myBookings.value = currentList
        }

        val currentWeekly = _weeklyBookings.value
        if (currentWeekly?.daySchedules != null) {
            val updatedMap = currentWeekly.daySchedules.mapValues { entry ->
                entry.value.map { item ->
                    if (!item.id.isNullOrEmpty() && item.id == bookingId) updatedBooking else item
                }
            }
            _weeklyBookings.value = currentWeekly.copy(daySchedules = updatedMap)
        }
    }

    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.acceptBooking(bookingId)) {
                is Resource.Success -> {
                    val booking = result.data.data
                    com.example.myapplication.utils.notification.AppointmentReminderScheduler.schedule30MinReminder(getApplication(), booking)
                    updateLocalBooking(booking)
                    _uiState.value = UiState.Success(booking)
                    _event.emit(UiEvent.ShowToast("Đã chấp nhận cuộc hẹn"))
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Không thể chấp nhận cuộc hẹn"
                    _uiState.value = UiState.Error(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
            }
        }
    }

    fun rejectBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.rejectBooking(bookingId)) {
                is Resource.Success -> {
                    com.example.myapplication.utils.notification.AppointmentReminderScheduler.cancelReminder(getApplication(), bookingId)
                    val booking = result.data.data
                    updateLocalBooking(booking)
                    _uiState.value = UiState.Success(booking)
                    _event.emit(UiEvent.ShowToast("Đã từ chối cuộc hẹn"))
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Không thể từ chối cuộc hẹn"
                    _uiState.value = UiState.Error(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
            }
        }
    }

    fun cancelBooking(bookingId: String, cancelReason: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.cancelBooking(bookingId, cancelReason)) {
                is Resource.Success -> {
                    com.example.myapplication.utils.notification.AppointmentReminderScheduler.cancelReminder(getApplication(), bookingId)
                    val booking = result.data.data
                    updateLocalBooking(booking)
                    _uiState.value = UiState.Success(booking)
                    _event.emit(UiEvent.ShowToast("Đã hủy cuộc hẹn"))
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Không thể hủy cuộc hẹn"
                    _uiState.value = UiState.Error(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
            }
        }
    }

    fun getBookingDetail(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getBookingDetail(bookingId)) {
                is Resource.Success -> {
                    _uiState.value = UiState.Success(result.data.data)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun completeBooking(bookingId: String) {
        viewModelScope.launch {
            android.util.Log.d("CHAT_BOOKING_DEBUG", "[COMPLETE_API_CALL] Calling completeBooking for bookingId=$bookingId")
            _uiState.value = UiState.Loading
            when (val result = repository.completeBooking(bookingId)) {
                is Resource.Success -> {
                    android.util.Log.d("CHAT_BOOKING_DEBUG", "[COMPLETE_API_SUCCESS] Booking $bookingId completed successfully on server: ${result.data.data.status}")
                    val booking = result.data.data
                    updateLocalBooking(booking)
                    _uiState.value = UiState.Success(booking)
                    _event.emit(UiEvent.ShowToast("Đã hoàn thành cuộc hẹn"))
                }
                is Resource.Error -> {
                    android.util.Log.e("CHAT_BOOKING_DEBUG", "[COMPLETE_API_ERROR] Failed to complete booking $bookingId: ${result.message}")
                    val msg = if (result.message?.contains("before its scheduled time", ignoreCase = true) == true) {
                        "Chưa đến thời gian thực hiện cuộc hẹn, không thể hoàn thành!"
                    } else {
                        result.message ?: "Không thể hoàn thành cuộc hẹn"
                    }
                    _uiState.value = UiState.Error(msg)
                    _event.emit(UiEvent.ShowToast(msg))
                }
            }
        }
    }

    fun rateBooking(bookingId: String, score: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.rateBooking(bookingId, score)) {
                is Resource.Success -> {
                    _event.emit(UiEvent.ShowToast("Đã gửi đánh giá thành công"))
                    getBookingDetail(bookingId)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }
}
