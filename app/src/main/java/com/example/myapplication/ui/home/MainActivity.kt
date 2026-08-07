package com.example.myapplication.ui.home

import android.content.Intent
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.home.map.MapFragment

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        val preferenceManager = PreferenceManager(this)
        if (!preferenceManager.isLogin()) {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        val token = preferenceManager.getAccessToken() ?: ""
        if (token.isNotEmpty()) {
            com.example.myapplication.data.remote.websocket.WebSocketManager.connect(
                "${com.example.myapplication.data.remote.network.NetworkConstants.WS_URL}?token=$token",
                token
            )
        }

        checkNotificationPermission()
        fetchAndSyncFcmToken()

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, MapFragment())
            .commit()
    }

    private fun fetchAndSyncFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        com.example.myapplication.utils.notification.FcmTokenManager.registerDeviceToken(this, token)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("MAIN_ACTIVITY", "Could not fetch FCM token", e)
        }
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun observeData() {
    }
}