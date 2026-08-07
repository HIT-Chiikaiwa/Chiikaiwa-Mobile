package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityFriendsListBinding
import com.example.myapplication.databinding.DialogFriendOptionsBinding
import com.example.myapplication.databinding.DialogUserInfoBinding
import com.example.myapplication.databinding.LayoutFriendsMenuPopupBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.FriendsAdapter
import com.example.myapplication.ui.home.chat.chatroom.ChatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val currentUserId = com.example.myapplication.data.local.PreferenceManager(this).getUserId()
                val isUserUnavailable = conversation.memberCount == 1 || conversation.hasLeft
                val name = when {
                    isUserUnavailable -> "Người dùng không tồn tại"
                    !conversation.groupName.isNullOrEmpty() -> conversation.groupName
                    conversation.lastMessage != null && conversation.lastMessage.senderId != currentUserId && !conversation.lastMessage.senderName.isNullOrEmpty() -> conversation.lastMessage.senderName
                    else -> "Người dùng"
                }
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("conversation_id", conversation.id)
                    putExtra("user_name", name)
                    putExtra("is_disabled", isUserUnavailable)
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
        val popupView: View = popupBinding.root
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 16f

        popupBinding.btnMenuNewChat.setOnClickListener {
            popupWindow.dismiss()
            startActivity(Intent(this, SearchUserActivity::class.java))
        }

        popupBinding.btnMenuPendingRequests.setOnClickListener {
            popupWindow.dismiss()
            startActivity(Intent(this, PendingRequestsActivity::class.java))
        }

        popupWindow.showAsDropDown(anchorView, -150, 0)
    }

    private fun showFriendOptionsDialog(conversation: com.example.myapplication.data.remote.dto.response.ConversationResponse) {
        val dialog = AlertDialog.Builder(this).create()
        val binding = DialogFriendOptionsBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"

        binding.tvOptionPin.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        binding.tvOptionCreateGroup.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        binding.tvOptionViewProfile.setOnClickListener {
            dialog.dismiss()
            val currentUserId = com.example.myapplication.data.local.PreferenceManager(this@FriendsListActivity).getUserId()
            val lastSenderId = conversation.lastMessage?.senderId
            if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId) {
                showUserInfoDialog(name, lastSenderId)
            } else {
                fetchPartnerIdFromMessages(conversation.id, name)
            }
        }

        binding.tvOptionBlock.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        binding.tvOptionDelete.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun fetchPartnerIdFromMessages(conversationId: String, name: String) {
        val currentUserId = com.example.myapplication.data.local.PreferenceManager(this).getUserId() ?: ""
        val messageRepository = com.example.myapplication.data.repository.MessageRepository(this)
        
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Đang tải thông tin...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                messageRepository.getMessages(conversationId, page = 0, size = 20)
            }
            progressDialog.dismiss()
            
            if (result is com.example.myapplication.utils.resource.Resource.Success) {
                val messages = result.data.data.content
                val partnerId = messages.firstOrNull { it.senderId != currentUserId }?.senderId
                if (!partnerId.isNullOrEmpty()) {
                    showUserInfoDialog(name, partnerId)
                } else {
                    showToast("Không tìm thấy thông tin đối phương")
                }
            } else {
                showToast("Lỗi tải thông tin: ${(result as? com.example.myapplication.utils.resource.Resource.Error)?.message}")
            }
        }
    }

    private fun showUserInfoDialog(userName: String, userId: String? = null) {
        val dialog = AlertDialog.Builder(this).create()
        val binding = DialogUserInfoBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        binding.tvUserName.text = userName.ifEmpty { "Người dùng" }
        binding.tvDistance.visibility = View.GONE
        binding.tvStatusTag.visibility = View.GONE
        binding.tvSchool.text = "Chưa cập nhật"
        binding.tvMajor.text = "Chưa cập nhật"

        binding.btnAddFriend.text = "Nhắn tin"
        binding.btnAddFriend.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("user_name", userName)
                if (!userId.isNullOrEmpty()) {
                    putExtra("target_user_id", userId)
                }
            }
            startActivity(intent)
        }

        binding.btnViewProfile.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, com.example.myapplication.ui.profile.ProfileActivity::class.java).apply {
                if (!userId.isNullOrEmpty()) {
                    putExtra("target_user_id", userId)
                }
            }
            startActivity(intent)
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
