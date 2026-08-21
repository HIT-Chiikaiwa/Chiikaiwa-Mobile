package com.example.myapplication.ui.schedule

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.databinding.DialogBookingDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BookingDetailDialog(
    private val context: Context,
    private val booking: BookingDto,
    private val viewModel: BookingViewModel,
    private val onStatusChanged: (newStatus: String?, reason: String?, rating: Int?) -> Unit = { _, _, _ -> }
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

        val duration = booking.durationMinutes ?: 30
        val formattedScheduledAt = formatScheduledAt(booking.scheduledAt)
        binding.tvTimeAndDate.text = "Thời gian: $formattedScheduledAt ($duration phút)"

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

        val preferenceManager = com.example.myapplication.data.local.PreferenceManager(context)
        val currentUserId = preferenceManager.getUserId()
        val isCreator = if (currentUserId.isNullOrEmpty()) true
        else if (!booking.creatorId.isNullOrEmpty()) booking.creatorId == currentUserId
        else if (!booking.partnerId.isNullOrEmpty()) booking.partnerId != currentUserId
        else true

        when (status.uppercase()) {
            "PENDING" -> {
                if (isCreator) {
                    binding.btnCancel.visibility = View.VISIBLE
                } else {
                    binding.btnAccept.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.VISIBLE
                }
            }
            "CONFIRMED", "ACCEPTED" -> {
                binding.btnComplete.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE
            }
            "COMPLETED" -> {
                binding.layoutRatingSection.visibility = View.VISIBLE
                if (booking.hasRated == true || booking.myRating != null) {
                    binding.ratingBar.rating = (booking.myRating ?: 5).toFloat()
                    binding.ratingBar.setIsIndicator(true)
                    binding.btnRate.visibility = View.GONE
                } else {
                    binding.ratingBar.setIsIndicator(false)
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
            onStatusChanged("ACCEPTED", null, null)
        }

        binding.btnReject.setOnClickListener {
            viewModel.rejectBooking(bookingId)
            dialog.dismiss()
            onStatusChanged("REJECTED", null, null)
        }

        binding.btnComplete.setOnClickListener {
            viewModel.completeBooking(bookingId)
            dialog.dismiss()
            onStatusChanged("COMPLETED", null, null)
        }

        binding.btnCancel.setOnClickListener {
            BookingDialogHelper.showCancelReasonDialog(context, bookingId, viewModel) { reason ->
                dialog.dismiss()
                onStatusChanged("CANCELLED", reason, null)
            }
        }

        binding.btnRate.setOnClickListener {
            BookingDialogHelper.handleRatingSubmit(context, booking, binding, viewModel) { score ->
                dialog.dismiss()
                onStatusChanged(null, null, score)
            }
        }
    }

    private fun formatScheduledAt(rawTime: String?): String {
        if (rawTime.isNullOrEmpty()) return "Chưa xác định"
        val result = com.example.myapplication.utils.TimeUtils.formatUtcToVn(rawTime, "HH:mm, dd/MM/yyyy")
        return result.ifEmpty { rawTime }
    }
}
