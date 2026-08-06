package com.example.myapplication.ui.profile

import android.content.Context
import com.example.myapplication.databinding.ActivityNotificationSettingsBinding
import com.example.myapplication.ui.base.BaseActivity

class NotificationSettingsActivity : BaseActivity<ActivityNotificationSettingsBinding>() {

    override fun inflateBinding() = ActivityNotificationSettingsBinding.inflate(layoutInflater)

    private val prefs by lazy {
        getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    }

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Restore saved switch states
        binding.switchSystemNotification.isChecked = prefs.getBoolean("system_notif", true)
        binding.switchAppointmentNotification.isChecked = prefs.getBoolean("appointment_notif", true)
        binding.switchScheduleNotification.isChecked = prefs.getBoolean("schedule_notif", false)

        // Listen for changes and persist state
        binding.switchSystemNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("system_notif", isChecked).apply()
            showToast(if (isChecked) "Đã bật thông báo hệ thống" else "Đã tắt thông báo hệ thống")
        }

        binding.switchAppointmentNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("appointment_notif", isChecked).apply()
            showToast(if (isChecked) "Đã bật thông báo Lịch hẹn" else "Đã tắt thông báo Lịch hẹn")
        }

        binding.switchScheduleNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("schedule_notif", isChecked).apply()
            showToast(if (isChecked) "Đã bật thông báo thời gian biểu" else "Đã tắt thông báo thời gian biểu")
        }
    }

    override fun observeData() {
        // Local preference storage only
    }
}
