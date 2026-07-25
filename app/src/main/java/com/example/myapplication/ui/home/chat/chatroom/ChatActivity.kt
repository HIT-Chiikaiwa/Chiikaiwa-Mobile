package com.example.myapplication.ui.home.chat.chatroom

import android.view.WindowManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.base.BaseActivity

class ChatActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val conversationId = intent.getStringExtra("conversation_id") ?: ""
        val targetUserId = intent.getStringExtra("target_user_id") ?: ""
        val userName = intent.getStringExtra("user_name") ?: ""

        val fragment = ChatFragment.newInstance(conversationId, targetUserId, userName)
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun observeData() {}
}
