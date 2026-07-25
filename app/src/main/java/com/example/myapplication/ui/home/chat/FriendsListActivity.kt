package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityFriendsListBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.FriendsAdapter
import com.example.myapplication.ui.home.chat.chatroom.ChatActivity

class FriendsListActivity : BaseActivity<ActivityFriendsListBinding>() {

    override fun inflateBinding() = ActivityFriendsListBinding.inflate(layoutInflater)

    private val viewModel: FriendsListViewModel by viewModels()
    private lateinit var adapter: FriendsAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = FriendsAdapter { conversation ->
            val targetUserId = conversation.lastMessage?.senderId ?: ""
            val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("conversation_id", conversation.id)
                putExtra("target_user_id", targetUserId)
                putExtra("user_name", name)
            }
            startActivity(intent)
        }

        binding.rvFriendsList.layoutManager = LinearLayoutManager(this)
        binding.rvFriendsList.adapter = adapter

        binding.etSearchFriends.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchConversations(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.fetchConversations()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    adapter.submitList(state.data)
                }
                is UiState.Error -> {
                    showToast(state.message)
                }
                else -> {}
            }
        }
    }
}
