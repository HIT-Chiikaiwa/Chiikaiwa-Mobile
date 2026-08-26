package com.example.myapplication.ui.chat

import android.content.Context
import android.util.Log
import android.view.View
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.ui.chat.component.ReactionPopup
import com.example.myapplication.ui.schedule.BookingDetailDialog
import com.example.myapplication.ui.schedule.BookingViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject

object ChatDialogManager {

    fun showBookingDetailDialog(
        context: Context,
        message: Message,
        bookingViewModel: BookingViewModel,
        viewModel: ChatViewModel,
        conversationId: String
    ) {
        try {
            val bookingDto = Gson().fromJson(message.content, BookingDto::class.java)
            val json = Gson().fromJson(message.content, JsonObject::class.java)
            val bId = if (bookingDto.id.isNullOrEmpty() && json.has("bookingId")) json.get("bookingId").asString else bookingDto.id
            val locName = if (bookingDto.locationName.isNullOrEmpty() && json.has("location")) json.get("location").asString else bookingDto.locationName
            val finalBookingDto = bookingDto.copy(id = bId, locationName = locName)
            BookingDetailDialog(
                context = context,
                booking = finalBookingDto,
                viewModel = bookingViewModel
            ) { newStatus, reason, rating ->
                finalBookingDto.id?.let { bId ->
                    if (!newStatus.isNullOrEmpty()) {
                        viewModel.updateBookingMessageStatus(bId, newStatus, reason)
                    }
                    if (rating != null) {
                        viewModel.updateBookingMessageRating(bId, rating)
                    }
                }
            }.show()
        } catch (e: Exception) {
            Log.e("ChatDialogManager", "Failed to show booking detail dialog", e)
        }
    }

    fun showActionPopup(
        context: Context,
        anchorView: View,
        message: Message,
        currentUserId: String,
        viewModel: ChatViewModel,
        bookingViewModel: BookingViewModel,
        conversationId: String
    ) {
        if (anchorView.id == com.example.myapplication.R.id.btnViewDetail) {
            showBookingDetailDialog(context, message, bookingViewModel, viewModel, conversationId)
            return
        }

        val popup = ReactionPopup(
            context = context,
            currentUserId = currentUserId,
            onRecallClick = { msg -> viewModel.recallMessage(msg.id) },
            onDeleteClick = { msg -> viewModel.deleteMessage(msg.id) },
            onReplyClick = { msg -> viewModel.setReplyingTo(msg) },
            onReactionClick = { msg, emoji -> viewModel.toggleReaction(msg.id, emoji) }
        )
        popup.show(anchorView, message)
    }
}
