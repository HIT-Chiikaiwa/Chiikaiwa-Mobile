package com.example.myapplication.ui.profile

import android.content.Context
import com.example.myapplication.R
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

        binding.switchSystemNotification.isChecked = prefs.getBoolean("system_notif", true)
        binding.switchAppointmentNotification.isChecked = prefs.getBoolean("appointment_notif", true)
        binding.switchScheduleNotification.isChecked = prefs.getBoolean("schedule_notif", true)

        binding.switchSystemNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("system_notif", isChecked).apply()
            showToast(if (isChecked) getString(R.string.toast_system_notif_on) else getString(R.string.toast_system_notif_off))
        }

        binding.switchAppointmentNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("appointment_notif", isChecked).apply()
            showToast(if (isChecked) getString(R.string.toast_appointment_notif_on) else getString(R.string.toast_appointment_notif_off))
        }

        binding.switchScheduleNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("schedule_notif", isChecked).apply()
            showToast(if (isChecked) getString(R.string.toast_schedule_notif_on) else getString(R.string.toast_schedule_notif_off))
        }
    }

    override fun observeData() {
    }
}
