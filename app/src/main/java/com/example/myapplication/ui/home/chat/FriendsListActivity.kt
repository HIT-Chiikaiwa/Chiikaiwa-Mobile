package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R
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
            fetchPartnerId(conversation) { partnerId ->
                showUserInfoDialog(name, partnerId)
            }
        }

        binding.tvOptionBlock.setOnClickListener {
            dialog.dismiss()
            fetchPartnerId(conversation) { partnerId ->
                showBlockConfirmDialog(partnerId, name)
            }
        }

        binding.tvOptionDelete.setOnClickListener {
            dialog.dismiss()
            fetchPartnerId(conversation) { partnerId ->
                showUnfriendConfirmDialog(partnerId, name, conversation.id)
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun fetchPartnerId(conversation: com.example.myapplication.data.remote.dto.response.ConversationResponse, callback: (partnerId: String) -> Unit) {
        val currentUserId = com.example.myapplication.data.local.PreferenceManager(this).getUserId() ?: ""
        val lastSenderId = conversation.lastMessage?.senderId
        if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId) {
            callback(lastSenderId)
            return
        }

        val messageRepository = com.example.myapplication.data.repository.MessageRepository(this)
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    messageRepository.getMessages(conversation.id, page = 0, size = 20)
                }
                if (result is com.example.myapplication.utils.resource.Resource.Success) {
                    val messages = result.data.data.content
                    val partnerId = messages.firstOrNull { it.senderId != currentUserId }?.senderId
                    if (!partnerId.isNullOrEmpty()) {
                        callback(partnerId)
                    } else {
                        showToast("Không tìm thấy thông tin đối phương")
                    }
                } else {
                    showToast("Lỗi tải thông tin: ${(result as? com.example.myapplication.utils.resource.Resource.Error)?.message}")
                }
            } catch (e: Exception) {
                showToast("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun showUnfriendConfirmDialog(friendId: String, friendName: String, conversationId: String) {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = com.example.myapplication.databinding.DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = "Hủy kết bạn"
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn hủy kết bạn với $friendName không?"
        dialogBinding.btnConfirm.text = "Hủy kết bạn"
        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.unfriend(friendId, conversationId) {
                showToast("Hủy kết bạn thành công")
            }
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showBlockConfirmDialog(userId: String, friendName: String) {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = com.example.myapplication.databinding.DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = "Chặn người dùng"
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn chặn $friendName không? Hai người sẽ không thể liên lạc với nhau."
        dialogBinding.btnConfirm.text = "Chặn"
        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.blockUser(userId) {
                showToast("Đã chặn $friendName")
            }
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
        binding.viewOnlineBadge.visibility = View.GONE
        binding.tvOnlineStatus.visibility = View.GONE

        if (!userId.isNullOrEmpty()) {
            val profileRepository = com.example.myapplication.data.repository.ProfileRepository(this)
            
            lifecycleScope.launch {
                when (val result = profileRepository.getProfile(userId)) {
                    is com.example.myapplication.utils.resource.Resource.Success -> {
                        val user = result.data.data
                        binding.tvSchool.text = user.university ?: "Chưa cập nhật"
                        binding.tvMajor.text = user.majorName ?: "Chưa cập nhật"
                        if (!user.statusTag.isNullOrEmpty()) {
                            binding.tvStatusTag.visibility = View.VISIBLE
                            binding.tvStatusTag.text = user.statusTag
                        }
                        if (!user.avatar.isNullOrEmpty()) {
                            com.bumptech.glide.Glide.with(binding.ivAvatar.context)
                                .load(user.avatar)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(binding.ivAvatar)
                        }
                    }
                    is com.example.myapplication.utils.resource.Resource.Error -> {}
                }
            }

            lifecycleScope.launch {
                when (val result = profileRepository.getUserOnlineStatus(userId)) {
                    is com.example.myapplication.utils.resource.Resource.Success -> {
                        val status = result.data.data
                        if (status.isOnline) {
                            binding.viewOnlineBadge.visibility = View.VISIBLE
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = "Đang hoạt động"
                        } else {
                            binding.viewOnlineBadge.visibility = View.GONE
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            if (!status.lastSeen.isNullOrBlank()) {
                                val relativeTime = com.example.myapplication.utils.TimeUtils.formatRelativeTime(status.lastSeen)
                                if (relativeTime == "Vừa xong" || relativeTime.contains("trước")) {
                                    binding.tvOnlineStatus.text = "Hoạt động $relativeTime"
                                } else {
                                    binding.tvOnlineStatus.text = "Hoạt động từ $relativeTime"
                                }
                            } else {
                                binding.tvOnlineStatus.text = "Ngoại tuyến"
                            }
                        }
                    }
                    is com.example.myapplication.utils.resource.Resource.Error -> {}
                }
            }
        }

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
