package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.CancelBookingRequest
import com.example.myapplication.data.remote.dto.request.CreateBookingRequest
import com.example.myapplication.data.remote.dto.request.RateBookingRequest
import com.example.myapplication.data.remote.dto.response.ActionStatusDto
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.data.remote.dto.response.WeeklyBookingResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class BookingRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun createBookingInConversation(
        conversationId: String,
        request: CreateBookingRequest
    ): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.createBookingInConversation(conversationId, request) }
    }

    suspend fun getMyBookings(): Resource<BaseResponse<List<BookingDto>>> {
        return safeApiCall { api.getMyBookings() }
    }

    suspend fun getWeeklyBookings(weekStart: String): Resource<BaseResponse<WeeklyBookingResponse>> {
        return safeApiCall { api.getWeeklyBookings(weekStart) }
    }

    suspend fun getBookingDetail(bookingId: String): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.getBookingDetail(bookingId) }
    }

    suspend fun acceptBooking(bookingId: String): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.acceptBooking(bookingId) }
    }

    suspend fun rejectBooking(bookingId: String): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.rejectBooking(bookingId) }
    }

    suspend fun completeBooking(bookingId: String): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.completeBooking(bookingId) }
    }

    suspend fun cancelBooking(
        bookingId: String,
        cancelReason: String?
    ): Resource<BaseResponse<BookingDto>> {
        return safeApiCall { api.cancelBooking(bookingId, CancelBookingRequest(cancelReason)) }
    }

    suspend fun rateBooking(
        bookingId: String,
        score: Int
    ): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.rateBooking(bookingId, RateBookingRequest(score)) }
    }
}
