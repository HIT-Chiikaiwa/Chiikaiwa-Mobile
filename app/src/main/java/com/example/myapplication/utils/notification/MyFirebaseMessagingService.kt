package com.example.myapplication.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        FcmTokenManager.registerDeviceToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""

        if (body.isEmpty() && title.isEmpty()) return

        val prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val type = remoteMessage.data["type"]?.uppercase() ?: remoteMessage.data["targetType"]?.uppercase() ?: ""

        val isShowNotification = when (type) {
            "FRIEND", "FRIEND_REQUEST" -> {
                prefs.getBoolean("system_notif", true)
            }
            "BOOKING", "APPOINTMENT" -> {
                prefs.getBoolean("appointment_notif", true)
            }
            "CHAT", "MESSAGE" -> {
                prefs.getBoolean("system_notif", true)
            }
            else -> {
                prefs.getBoolean("system_notif", true)
            }
        }

        if (isShowNotification) {
            showNotification(title, body, remoteMessage.data)
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = "push_notifications_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo đẩy hệ thống",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh nhận thông báo từ máy chủ"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "FCM_SERVICE"
    }
}
