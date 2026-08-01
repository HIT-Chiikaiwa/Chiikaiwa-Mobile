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

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, MapFragment())
            .commit()
    }

    override fun observeData() {
    }
}