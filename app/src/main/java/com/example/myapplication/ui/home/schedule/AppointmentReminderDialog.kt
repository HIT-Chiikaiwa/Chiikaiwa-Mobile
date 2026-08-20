package com.example.myapplication.ui.home.schedule

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import com.example.myapplication.databinding.DialogAppointmentReminderBinding
import com.example.myapplication.utils.TimeUtils

class AppointmentReminderDialog(
    context: Context,
    private val subject: String,
    private val scheduledAt: String,
    private val location: String? = null
) : Dialog(context) {

    private lateinit var binding: DialogAppointmentReminderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAppointmentReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvSubject.text = subject.ifEmpty { "Cuộc hẹn học tập" }

        val timeFormatted = if (scheduledAt.isNotEmpty()) {
            TimeUtils.formatUtcToVn(scheduledAt, "HH:mm, dd/MM/yyyy")
        } else "Chưa xác định"
        binding.tvTime.text = "Thời gian: $timeFormatted"

        if (!location.isNullOrEmpty()) {
            binding.tvLocation.text = "Địa điểm: $location"
            binding.tvLocation.visibility = View.VISIBLE
        } else {
            binding.tvLocation.visibility = View.GONE
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnViewSchedule.setOnClickListener {
            dismiss()
            val intent = Intent(context, com.example.myapplication.ui.home.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("type", "BOOKING")
            }
            context.startActivity(intent)
        }
    }
}
