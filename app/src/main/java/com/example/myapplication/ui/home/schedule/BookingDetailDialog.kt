package com.example.myapplication.ui.home.schedule

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.databinding.DialogBookingDetailBinding

class BookingDetailDialog(
    private val context: Context,
    private val booking: BookingDto,
    private val viewModel: BookingViewModel,
    private val onStatusChanged: () -> Unit
) {

    private val dialog = Dialog(context)
    private val binding = DialogBookingDetailBinding.inflate(LayoutInflater.from(context))

    fun show() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        bindData()
        setupListeners()

        dialog.show()
    }

    private fun bindData() {
        binding.tvSubject.text = booking.subject ?: "Cuộc hẹn"
        val partnerName = booking.partnerName ?: booking.creatorName ?: "Người dùng"
        binding.tvPartnerName.text = "Với: $partnerName"

        val status = booking.status ?: "PENDING"
        binding.tvStatus.text = status

        val duration = booking.durationMinutes ?: 60
        val timeStr = booking.scheduledAt ?: ""
        binding.tvTimeAndDate.text = "Thời gian: $timeStr ($duration phút)"

        val locName = booking.locationName ?: "Online"
        val locAddr = booking.locationAddress ?: ""
        binding.tvLocation.text = "Địa điểm: $locName $locAddr".trim()

        binding.tvNote.text = "Ghi chú: ${booking.note ?: "Không có"}"

        val avatarUrl = booking.partnerAvatar ?: booking.creatorAvatar
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.imgPartnerAvatar)
        }

        binding.btnAccept.visibility = View.GONE
        binding.btnReject.visibility = View.GONE
        binding.btnComplete.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.layoutRatingSection.visibility = View.GONE

        when (status.uppercase()) {
            "PENDING" -> {
                binding.btnAccept.visibility = View.VISIBLE
                binding.btnReject.visibility = View.VISIBLE
            }
            "CONFIRMED", "ACCEPTED" -> {
                binding.btnComplete.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE
            }
            "COMPLETED" -> {
                binding.layoutRatingSection.visibility = View.VISIBLE
                if (booking.hasRated == true && booking.myRating != null) {
                    binding.ratingBar.rating = booking.myRating.toFloat()
                    binding.btnRate.visibility = View.GONE
                } else {
                    binding.btnRate.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        val bookingId = booking.id ?: return

        binding.btnAccept.setOnClickListener {
            viewModel.acceptBooking(bookingId)
            dialog.dismiss()
            onStatusChanged()
        }

        binding.btnReject.setOnClickListener {
            viewModel.rejectBooking(bookingId)
            dialog.dismiss()
            onStatusChanged()
        }

        binding.btnComplete.setOnClickListener {
            viewModel.completeBooking(bookingId)
            dialog.dismiss()
            onStatusChanged()
        }

        binding.btnCancel.setOnClickListener {
            showCancelReasonDialog(bookingId)
        }

        binding.btnRate.setOnClickListener {
            val score = binding.ratingBar.rating.toInt()
            viewModel.rateBooking(bookingId, score)
            dialog.dismiss()
            onStatusChanged()
        }
    }

    private fun showCancelReasonDialog(bookingId: String) {
        val input = EditText(context).apply {
            hint = "Nhập lý do hủy hẹn..."
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(context)
            .setTitle("Hủy cuộc hẹn")
            .setView(input)
            .setPositiveButton("Hủy hẹn") { _, _ ->
                val reason = input.text.toString().trim()
                viewModel.cancelBooking(bookingId, reason)
                dialog.dismiss()
                onStatusChanged()
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }
}
