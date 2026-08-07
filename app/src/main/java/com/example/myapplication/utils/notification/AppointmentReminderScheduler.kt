package com.example.myapplication.utils.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.utils.TimeUtils

object AppointmentReminderScheduler {

    fun schedule30MinReminder(context: Context, booking: BookingDto) {
        val bookingId = booking.id ?: return
        val scheduledAt = booking.scheduledAt ?: return

        val date = TimeUtils.parseUtcDate(scheduledAt) ?: return
        val scheduledTimeMillis = date.time
        val reminderTimeMillis = scheduledTimeMillis - (30 * 60 * 1000L)

        if (reminderTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra(AppointmentReminderReceiver.EXTRA_BOOKING_ID, bookingId)
            putExtra(AppointmentReminderReceiver.EXTRA_TITLE, booking.subject ?: "Học tập")
            putExtra(AppointmentReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, bookingId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
