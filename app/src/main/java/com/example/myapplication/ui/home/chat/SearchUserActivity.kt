package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivitySearchUserBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.UserSearchAdapter
import com.example.myapplication.ui.home.chat.chatroom.ChatActivity

class SearchUserActivity : BaseActivity<ActivitySearchUserBinding>() {

    override fun inflateBinding() = ActivitySearchUserBinding.inflate(layoutInflater)

    private val friendRequestViewModel: FriendRequestViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()
    private lateinit var searchAdapter: UserSearchAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        searchAdapter = UserSearchAdapter(
            onSendFriendRequest = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                friendRequestViewModel.sendFriendRequest(targetId) {
                    showToast("Đã gửi yêu cầu kết bạn")
                }
            },
            onStartChat = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                conversationViewModel.startDirectChat(targetId) { convId, userName ->
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("conversation_id", convId)
                        putExtra("target_user_id", targetId)
                        putExtra("user_name", userName)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        )

        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter

        val performSearch = {
            val kw = binding.edtKeyword.text.toString().trim()
            if (kw.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                friendRequestViewModel.searchUsers(kw) { results ->
                    binding.progressBar.visibility = View.GONE
                    searchAdapter.submitList(results)
                }
            }
        }

        binding.btnSearch.setOnClickListener { performSearch() }
        binding.edtKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    override fun observeData() {
        friendRequestViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }

        conversationViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }
    }
}
