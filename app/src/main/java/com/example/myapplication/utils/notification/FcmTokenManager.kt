package com.example.myapplication.utils.notification

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.myapplication.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenManager {
    private const val TAG = "FCM_TOKEN_MANAGER"

    fun registerDeviceToken(context: Context, fcmToken: String) {
        if (fcmToken.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NotificationRepository(context)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val result = repository.registerDevice(fcmToken, deviceName)
                Log.d(TAG, "registerDevice result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM device token", e)
            }
        }
    }
}
