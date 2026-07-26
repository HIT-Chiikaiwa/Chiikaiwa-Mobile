package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

        binding.btnNewChat.setOnClickListener {
            showNewChatDialog()
        }

        adapter = FriendsAdapter { conversation ->
            val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("conversation_id", conversation.id)
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
    }

    private fun showNewChatDialog() {
        val input = EditText(this).apply {
            hint = "Nhập User ID"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Chat mới")
            .setMessage("Nhập User ID của người bạn muốn nhắn tin:")
            .setView(input)
            .setPositiveButton("Chat") { _, _ ->
                val userId = input.text.toString().trim()
                if (userId.isNotEmpty()) {
                    viewModel.startDirectChat(userId) { convId, userName ->
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra("conversation_id", convId)
                            putExtra("target_user_id", userId)
                            putExtra("user_name", userName)
                        }
                        startActivity(intent)
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchConversations()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    adapter.submitList(ArrayList(state.data))
                }
                is UiState.Error -> {
                    showToast(state.message)
                }
                else -> {}
            }
        }
    }
}
