package com.example.myapplication.ui.profile

import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityBlockedUsersBinding
import com.example.myapplication.databinding.DialogConfirmDeleteBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.BlockUserViewModel
import com.example.myapplication.ui.home.chat.adapter.BlockedUsersAdapter

class BlockedUsersActivity : BaseActivity<ActivityBlockedUsersBinding>() {

    override fun inflateBinding() = ActivityBlockedUsersBinding.inflate(layoutInflater)

    private val blockUserViewModel: BlockUserViewModel by viewModels()
    private lateinit var blockedAdapter: BlockedUsersAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        blockedAdapter = BlockedUsersAdapter(
            onUnblockClick = { user ->
                val userId = user.id ?: return@BlockedUsersAdapter
                val fullName = "${user.lastName ?: ""} ${user.firstName ?: ""}".trim()
                showUnblockConfirmDialog(userId, fullName.ifEmpty { "Người dùng" })
            }
        )

        binding.rvBlockedUsers.layoutManager = LinearLayoutManager(this)
        binding.rvBlockedUsers.adapter = blockedAdapter

        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        blockUserViewModel.getBlockedUsers { list ->
            binding.progressBar.visibility = View.GONE
            if (list.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvBlockedUsers.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvBlockedUsers.visibility = View.VISIBLE
                blockedAdapter.submitList(list)
            }
        }
    }

    private fun showUnblockConfirmDialog(userId: String, name: String) {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = "Bỏ chặn người dùng"
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn bỏ chặn $name không?"
        dialogBinding.btnConfirm.text = "Bỏ chặn"
        dialogBinding.btnConfirm.setOnClickListener {
            blockUserViewModel.unblockUser(userId) {
                showToast("Đã bỏ chặn $name")
                loadData()
            }
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun observeData() {
        blockUserViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }
    }
}
