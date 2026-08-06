package com.example.myapplication.ui.home.schedule

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.databinding.DialogBookingDetailBinding
import com.example.myapplication.databinding.DialogCancelReasonBinding

object BookingDialogHelper {

    fun showCancelReasonDialog(
        context: Context,
        bookingId: String,
        viewModel: BookingViewModel,
        onCancelled: (reason: String) -> Unit
    ) {
        val dialog = AlertDialog.Builder(context).create()
        val binding = DialogCancelReasonBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.root)

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirmCancel.setOnClickListener {
            val reason = binding.etCancelReason.text.toString().trim()
            viewModel.cancelBooking(bookingId, reason)
            onCancelled(reason)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun handleRatingSubmit(
        context: Context,
        booking: BookingDto,
        binding: DialogBookingDetailBinding,
        viewModel: BookingViewModel,
        onSuccess: (score: Int) -> Unit
    ) {
        val bookingId = booking.id ?: return

        if (booking.hasRated == true || booking.myRating != null) {
            Toast.makeText(context, "Bạn đã đánh giá cuộc hẹn này rồi và không thể chỉnh sửa.", Toast.LENGTH_SHORT).show()
            binding.ratingBar.setIsIndicator(true)
            binding.btnRate.visibility = android.view.View.GONE
            return
        }

        val score = binding.ratingBar.rating.toInt()
        if (score <= 0) {
            Toast.makeText(context, "Vui lòng chọn số sao để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRate.isEnabled = false
        binding.ratingBar.setIsIndicator(true)

        viewModel.rateBooking(bookingId, score)
        booking.hasRated = true
        booking.myRating = score

        binding.btnRate.visibility = android.view.View.GONE
        Toast.makeText(context, "Đã gửi đánh giá thành công!", Toast.LENGTH_SHORT).show()
        onSuccess(score)
    }
}
