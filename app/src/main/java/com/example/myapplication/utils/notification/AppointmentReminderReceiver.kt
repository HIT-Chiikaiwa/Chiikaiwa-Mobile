package com.example.myapplication.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.ui.main.MainActivity
import com.example.myapplication.utils.TimeUtils

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val appointmentNotifEnabled = prefs.getBoolean("appointment_notif", true)
        if (!appointmentNotifEnabled) return

        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Cuộc hẹn"
        val scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT) ?: ""

        val timeStr = if (scheduledAt.isNotEmpty()) {
            TimeUtils.formatUtcToVn(scheduledAt, "HH:mm, dd/MM/yyyy")
        } else ""

        val channelId = "appointment_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở cuộc hẹn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh gửi thông báo nhắc nhở cuộc hẹn sắp diễn ra"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_reminder_dialog", true)
            putExtra("reminder_title", title)
            putExtra("reminder_scheduled_at", scheduledAt)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            bookingId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (timeStr.isNotEmpty()) {
            context.getString(R.string.reminder_push_content_format, title, timeStr)
        } else {
            context.getString(R.string.reminder_push_content_short_format, title)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_push_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(bookingId.hashCode(), notification)
    }

    companion object {
        const val EXTRA_BOOKING_ID = "extra_booking_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"
    }
}
