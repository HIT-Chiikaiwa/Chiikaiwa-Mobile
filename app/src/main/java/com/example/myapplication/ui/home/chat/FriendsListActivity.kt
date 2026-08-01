package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityFriendsListBinding
import com.example.myapplication.databinding.DialogFriendOptionsBinding
import com.example.myapplication.databinding.LayoutFriendsMenuPopupBinding
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

        binding.btnHeaderMenu.setOnClickListener { view ->
            showHeaderMenuPopup(view)
        }

        adapter = FriendsAdapter(
            onItemClick = { conversation ->
                val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("conversation_id", conversation.id)
                    putExtra("user_name", name)
                }
                startActivity(intent)
            },
            onMoreClick = { conversation, _ ->
                showFriendOptionsDialog(conversation)
            }
        )

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

    private fun showHeaderMenuPopup(anchorView: View) {
        val popupBinding = LayoutFriendsMenuPopupBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 16f

        popupBinding.btnMenuNewChat.setOnClickListener {
            popupWindow.dismiss()
            showNewChatDialog()
        }

        popupBinding.btnMenuPendingRequests.setOnClickListener {
            popupWindow.dismiss()
            showPendingRequestsDialog()
        }

        popupWindow.showAsDropDown(anchorView, -150, 0)
    }

    private fun showFriendOptionsDialog(conversation: com.example.myapplication.data.remote.dto.response.ConversationResponse) {
        val dialog = AlertDialog.Builder(this).create()
        val binding = DialogFriendOptionsBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"
        binding.layoutFriendItemPreview.tvFriendName.text = name
        binding.layoutFriendItemPreview.tvLastMessage.text = conversation.lastMessage?.content ?: "Chưa có tin nhắn"
        binding.layoutFriendItemPreview.btnMore.visibility = View.GONE

        binding.tvOptionPin.setOnClickListener {
            showToast("Đã ghim cuộc hội thoại")
            dialog.dismiss()
        }

        binding.tvOptionCreateGroup.setOnClickListener {
            showToast("Tính năng tạo nhóm đang phát triển")
            dialog.dismiss()
        }

        binding.tvOptionViewProfile.setOnClickListener {
            showToast("Trang cá nhân của $name")
            dialog.dismiss()
        }

        binding.tvOptionBlock.setOnClickListener {
            showToast("Đã chặn $name")
            dialog.dismiss()
        }

        binding.tvOptionDelete.setOnClickListener {
            showToast("Đã xóa cuộc trò chuyện")
            dialog.dismiss()
        }

        dialog.show()
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
                binding.progressBar.visibility = View.VISIBLE
                viewModel.searchUsers(kw) { results ->
                    binding.progressBar.visibility = View.GONE
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
            binding.progressBar.visibility = View.VISIBLE
            viewModel.getPendingFriendRequests { list ->
                binding.progressBar.visibility = View.GONE
                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvPendingRequests.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvPendingRequests.visibility = View.VISIBLE
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
