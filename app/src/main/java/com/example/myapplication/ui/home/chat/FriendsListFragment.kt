package com.example.myapplication.ui.home.chat

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.repository.MessageRepository
import com.example.myapplication.data.repository.ProfileRepository
import com.example.myapplication.databinding.FragmentFriendsListBinding
import com.example.myapplication.databinding.DialogConfirmDeleteBinding
import com.example.myapplication.databinding.DialogFriendOptionsBinding
import com.example.myapplication.databinding.DialogUserInfoBinding
import com.example.myapplication.databinding.LayoutFriendsMenuPopupBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.FriendsAdapter
import com.example.myapplication.ui.profile.ProfileActivity
import com.example.myapplication.utils.TimeUtils
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendsListFragment : BaseFragment<FragmentFriendsListBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFriendsListBinding.inflate(inflater, container, false)

    private val conversationViewModel: ConversationViewModel by viewModels()
    private val friendRequestViewModel: FriendRequestViewModel by viewModels()
    private val blockUserViewModel: BlockUserViewModel by viewModels()

    private lateinit var adapter: FriendsAdapter
    private val preferenceManager by lazy { PreferenceManager(requireContext()) }

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnHeaderMenu.setOnClickListener { view ->
            showHeaderMenuPopup(view)
        }

        adapter = FriendsAdapter(
            onItemClick = { conversation ->
                val currentUserId = preferenceManager.getUserId()
                val isUserUnavailable = conversation.memberCount == 1 || conversation.hasLeft
                val name = when {
                    isUserUnavailable -> "Người dùng không tồn tại"
                    !conversation.groupName.isNullOrEmpty() -> conversation.groupName
                    conversation.lastMessage != null && conversation.lastMessage.senderId != currentUserId && !conversation.lastMessage.senderName.isNullOrEmpty() -> conversation.lastMessage.senderName
                    else -> "Người dùng"
                }
                val bundle = android.os.Bundle().apply {
                    putString("conversation_id", conversation.id)
                    putString("user_name", name)
                    putBoolean("is_disabled", isUserUnavailable)
                }
                findNavController().navigate(R.id.chatFragment, bundle)
            },
            onMoreClick = { conversation, _ ->
                showFriendOptionsDialog(conversation)
            }
        )

        binding.rvFriendsList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriendsList.adapter = adapter

        binding.etSearchFriends.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                conversationViewModel.searchConversations(s?.toString() ?: "")
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
            findNavController().navigate(R.id.searchUserFragment)
        }

        popupBinding.btnMenuPendingRequests.setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(R.id.pendingRequestsFragment)
        }

        popupWindow.showAsDropDown(anchorView, -150, 0)
    }

    private fun showFriendOptionsDialog(conversation: ConversationResponse) {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogBinding = DialogFriendOptionsBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        val name = conversation.groupName ?: conversation.lastMessage?.senderName ?: "Người dùng"

        dialogBinding.tvOptionPin.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        dialogBinding.tvOptionCreateGroup.setOnClickListener {
            showToast("Tính năng đang được phát triển")
            dialog.dismiss()
        }

        dialogBinding.tvOptionViewProfile.setOnClickListener {
            dialog.dismiss()
            fetchPartnerId(conversation) { partnerId ->
                showUserInfoDialog(name, partnerId)
            }
        }

        dialogBinding.tvOptionBlock.setOnClickListener {
            dialog.dismiss()
            fetchPartnerId(conversation) { partnerId ->
                showBlockConfirmDialog(partnerId, name)
            }
        }

        dialogBinding.tvOptionDelete.setOnClickListener {
            dialog.dismiss()
            fetchPartnerId(conversation) { partnerId ->
                showUnfriendConfirmDialog(partnerId, name, conversation.id)
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun fetchPartnerId(conversation: ConversationResponse, callback: (partnerId: String) -> Unit) {
        val currentUserId = preferenceManager.getUserId() ?: ""
        val lastSenderId = conversation.lastMessage?.senderId
        if (!lastSenderId.isNullOrEmpty() && lastSenderId != currentUserId) {
            callback(lastSenderId)
            return
        }

        val messageRepository = MessageRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    messageRepository.getMessages(conversation.id, page = 0, size = 20)
                }
                if (result is Resource.Success) {
                    val messages = result.data.data.content
                    val partnerId = messages.firstOrNull { it.senderId != currentUserId }?.senderId
                    if (!partnerId.isNullOrEmpty()) {
                        callback(partnerId)
                    } else {
                        showToast("Không tìm thấy thông tin đối phương")
                    }
                } else {
                    showToast("Lỗi tải thông tin: ${(result as? Resource.Error)?.message}")
                }
            } catch (e: Exception) {
                showToast("Lỗi kết nối: ${e.message}")
            }
        }
    }

    private fun showUnfriendConfirmDialog(friendId: String, friendName: String, conversationId: String) {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = "Hủy kết bạn"
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn hủy kết bạn với $friendName không?"
        dialogBinding.btnConfirm.text = "Hủy kết bạn"
        dialogBinding.btnConfirm.setOnClickListener {
            friendRequestViewModel.unfriend(friendId, conversationId) {
                showToast("Hủy kết bạn thành công")
                conversationViewModel.fetchConversations()
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
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = "Chặn người dùng"
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn chặn $friendName không? Hai người sẽ không thể liên lạc với nhau."
        dialogBinding.btnConfirm.text = "Chặn"
        dialogBinding.btnConfirm.setOnClickListener {
            blockUserViewModel.blockUser(userId) {
                showToast("Đã chặn $friendName")
                conversationViewModel.fetchConversations()
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
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogBinding = DialogUserInfoBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvUserName.text = userName.ifEmpty { "Người dùng" }
        dialogBinding.tvDistance.visibility = View.GONE
        dialogBinding.tvStatusTag.visibility = View.GONE
        dialogBinding.tvSchool.text = "Chưa cập nhật"
        dialogBinding.tvMajor.text = "Chưa cập nhật"
        dialogBinding.viewOnlineBadge.visibility = View.GONE
        dialogBinding.tvOnlineStatus.visibility = View.GONE

        if (!userId.isNullOrEmpty()) {
            val profileRepository = ProfileRepository(requireContext())

            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = profileRepository.getProfile(userId)) {
                    is Resource.Success -> {
                        val user = result.data.data
                        dialogBinding.tvSchool.text = user.university ?: "Chưa cập nhật"
                        dialogBinding.tvMajor.text = user.majorName ?: "Chưa cập nhật"
                        if (!user.statusTag.isNullOrEmpty()) {
                            dialogBinding.tvStatusTag.visibility = View.VISIBLE
                            dialogBinding.tvStatusTag.text = user.statusTag
                        }
                        if (!user.avatar.isNullOrEmpty()) {
                            Glide.with(dialogBinding.ivAvatar.context)
                                .load(user.avatar)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(dialogBinding.ivAvatar)
                        }
                    }
                    is Resource.Error -> {}
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = profileRepository.getUserOnlineStatus(userId)) {
                    is Resource.Success -> {
                        val status = result.data.data
                        if (status.isOnline) {
                            dialogBinding.viewOnlineBadge.visibility = View.VISIBLE
                            dialogBinding.tvOnlineStatus.visibility = View.VISIBLE
                            dialogBinding.tvOnlineStatus.text = "Đang hoạt động"
                        } else {
                            dialogBinding.viewOnlineBadge.visibility = View.GONE
                            dialogBinding.tvOnlineStatus.visibility = View.VISIBLE
                            if (!status.lastSeen.isNullOrBlank()) {
                                val relativeTime = TimeUtils.formatRelativeTime(status.lastSeen)
                                if (relativeTime == "Vừa xong" || relativeTime.contains("trước")) {
                                    dialogBinding.tvOnlineStatus.text = "Hoạt động $relativeTime"
                                } else {
                                    dialogBinding.tvOnlineStatus.text = "Hoạt động từ $relativeTime"
                                }
                            } else {
                                dialogBinding.tvOnlineStatus.text = "Ngoại tuyến"
                            }
                        }
                    }
                    is Resource.Error -> {}
                }
            }
        }

        dialogBinding.btnAddFriend.text = "Nhắn tin"
        dialogBinding.btnAddFriend.setOnClickListener {
            dialog.dismiss()
            val bundle = android.os.Bundle().apply {
                putString("user_name", userName)
                if (!userId.isNullOrEmpty()) {
                    putString("target_user_id", userId)
                }
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }

        dialogBinding.btnViewProfile.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), ProfileActivity::class.java).apply {
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
        conversationViewModel.fetchConversations()
    }

    override fun observeData() {
        conversationViewModel.uiState.observeState { state ->
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

        friendRequestViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }

        blockUserViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }
    }
}
