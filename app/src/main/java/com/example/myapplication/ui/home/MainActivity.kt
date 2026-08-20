package com.example.myapplication.ui.home

import android.content.Intent
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.ui.auth.AuthActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.home.map.MapFragment

import androidx.navigation.findNavController
import androidx.core.os.bundleOf
import com.example.myapplication.R

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        val preferenceManager = PreferenceManager(this)
        if (!preferenceManager.isLogin()) {
            val intent = Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        val token = preferenceManager.getAccessToken() ?: ""
        val userId = preferenceManager.getUserId() ?: ""
        if (token.isNotEmpty() && userId.isNotEmpty()) {
            com.example.myapplication.data.remote.websocket.WebSocketManager.connect(
                url = com.example.myapplication.data.remote.network.NetworkConstants.WS_URL,
                tokenProvider = { preferenceManager.getAccessToken() ?: "" },
                refreshTokenProvider = { preferenceManager.getRefreshToken() ?: "" },
                tokenSaver = { newAccess, newRefresh ->
                    preferenceManager.saveLogin(
                        accessToken = newAccess,
                        refreshToken = newRefresh,
                        userId = userId,
                        email = preferenceManager.getEmail()
                    )
                }
            )
        }

        checkNotificationPermission()
        fetchAndSyncFcmToken()

        binding.root.post {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.root.post {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val navController = try {
            findNavController(R.id.nav_host_fragment_main)
        } catch (e: Exception) {
            return
        }

        val bookingId = intent.getStringExtra("booking_id") ?: intent.getStringExtra("targetId")
        val type = intent.getStringExtra("type") ?: intent.getStringExtra("targetType")

        if (intent.getBooleanExtra("show_reminder_dialog", false)) {
            val title = intent.getStringExtra("reminder_title") ?: "Cuộc hẹn"
            val scheduledAt = intent.getStringExtra("reminder_scheduled_at") ?: ""
            val bundle = bundleOf(
                "show_reminder_dialog" to true,
                "reminder_title" to title,
                "reminder_scheduled_at" to scheduledAt
            )
            navController.navigate(R.id.notificationFragment, bundle)
            return
        }

        if (intent.hasExtra("id") || intent.hasExtra("type") || intent.hasExtra("targetType") || intent.hasExtra("show_reminder_dialog")) {
            when (type?.uppercase()) {
                "FRIEND", "FRIEND_REQUEST", "USER" -> {
                    val targetId = intent.getStringExtra("targetId") ?: intent.getStringExtra("actorId")
                    navController.navigate(R.id.profileFragment, bundleOf("target_user_id" to targetId))
                }
                "BOOKING", "APPOINTMENT" -> {
                    navController.navigate(R.id.scheduleFragment, bundleOf("booking_id" to bookingId))
                }
                "CHAT", "MESSAGE", "CONVERSATION" -> {
                    navController.navigate(R.id.friendsListFragment)
                }
                else -> {
                    navController.navigate(R.id.notificationFragment)
                }
            }
        }
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