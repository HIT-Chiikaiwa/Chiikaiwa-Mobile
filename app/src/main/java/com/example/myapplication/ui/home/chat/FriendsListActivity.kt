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

        binding.btnPendingRequests.setOnClickListener {
            showPendingRequestsDialog()
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
        val dialog = AlertDialog.Builder(this).create()
        val binding = com.example.myapplication.databinding.DialogSearchUserBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        val searchAdapter = com.example.myapplication.ui.home.chat.adapter.UserSearchAdapter(
            onSendFriendRequest = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                viewModel.sendFriendRequest(targetId) {
                    showToast("Đã gửi yêu cầu kết bạn")
                    dialog.dismiss()
                }
            },
            onStartChat = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                viewModel.startDirectChat(targetId) { convId, userName ->
                    dialog.dismiss()
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("conversation_id", convId)
                        putExtra("target_user_id", targetId)
                        putExtra("user_name", userName)
                    }
                    startActivity(intent)
                }
            }
        )

        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter

        val performSearch = {
            val kw = binding.edtKeyword.text.toString().trim()
            if (kw.isNotEmpty()) {
                binding.progressBar.visibility = android.view.View.VISIBLE
                viewModel.searchUsers(kw) { results ->
                    binding.progressBar.visibility = android.view.View.GONE
                    searchAdapter.submitList(results)
                }
            }
        }

        binding.btnBack.setOnClickListener { dialog.dismiss() }
        binding.btnSearch.setOnClickListener { performSearch() }
        binding.edtKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        dialog.show()
    }

    private fun showPendingRequestsDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val binding = com.example.myapplication.databinding.DialogPendingRequestsBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        lateinit var pendingAdapter: com.example.myapplication.ui.home.chat.adapter.PendingRequestsAdapter

        fun loadData() {
            binding.progressBar.visibility = android.view.View.VISIBLE
            viewModel.getPendingFriendRequests { list ->
                binding.progressBar.visibility = android.view.View.GONE
                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = android.view.View.VISIBLE
                    binding.rvPendingRequests.visibility = android.view.View.GONE
                } else {
                    binding.tvEmptyState.visibility = android.view.View.GONE
                    binding.rvPendingRequests.visibility = android.view.View.VISIBLE
                    pendingAdapter.submitList(list)
                }
            }
        }

        pendingAdapter = com.example.myapplication.ui.home.chat.adapter.PendingRequestsAdapter(
            onAccept = { item ->
                val reqId = item.requestId ?: return@PendingRequestsAdapter
                viewModel.acceptFriendRequest(reqId) {
                    showToast("Đã đồng ý kết bạn với ${item.lastName ?: ""} ${item.firstName ?: ""}")
                    loadData()
                    viewModel.fetchConversations()
                }
            },
            onReject = { item ->
                val reqId = item.requestId ?: return@PendingRequestsAdapter
                viewModel.rejectFriendRequest(reqId) {
                    showToast("Đã từ chối lời mời kết bạn")
                    loadData()
                }
            }
        )

        binding.btnBack.setOnClickListener { dialog.dismiss() }
        binding.rvPendingRequests.layoutManager = LinearLayoutManager(this)
        binding.rvPendingRequests.adapter = pendingAdapter

        loadData()
        dialog.show()
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
