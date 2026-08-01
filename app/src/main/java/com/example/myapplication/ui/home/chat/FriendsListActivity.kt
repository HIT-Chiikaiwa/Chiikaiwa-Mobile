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
import com.example.myapplication.databinding.DialogUserInfoBinding
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
            showToast("Đã ghim cuộc hội thoại")
            dialog.dismiss()
        }

        binding.tvOptionCreateGroup.setOnClickListener {
            showToast("Tính năng tạo nhóm đang phát triển")
            dialog.dismiss()
        }

        binding.tvOptionViewProfile.setOnClickListener {
            dialog.dismiss()
            showUserInfoDialog(name)
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
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showUserInfoDialog(userName: String) {
        val dialog = AlertDialog.Builder(this).create()
        val binding = DialogUserInfoBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        binding.tvUserName.text = userName.ifEmpty { "Người dùng" }
        binding.tvDistance.visibility = View.GONE
        binding.tvStatusTag.visibility = View.GONE
        binding.tvSchool.text = "Chưa cập nhật"
        binding.tvMajor.text = "Chưa cập nhật"

        binding.btnSendMessage.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("user_name", userName)
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
