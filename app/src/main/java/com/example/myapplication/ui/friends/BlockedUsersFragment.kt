package com.example.myapplication.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentBlockedUsersBinding
import com.example.myapplication.databinding.DialogConfirmDeleteBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.friends.BlockUserViewModel
import com.example.myapplication.ui.friends.adapter.BlockedUsersAdapter

class BlockedUsersFragment : BaseFragment<FragmentBlockedUsersBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBlockedUsersBinding.inflate(inflater, container, false)

    private val blockUserViewModel: BlockUserViewModel by viewModels()
    private lateinit var blockedAdapter: BlockedUsersAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        blockedAdapter = BlockedUsersAdapter(
            onUnblockClick = { user ->
                val userId = user.id ?: return@BlockedUsersAdapter
                val fullName = "${user.lastName ?: ""} ${user.firstName ?: ""}".trim()
                showUnblockConfirmDialog(userId, fullName.ifEmpty { "Người dùng" })
            }
        )

        binding.rvBlockedUsers.layoutManager = LinearLayoutManager(requireContext())
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
        val dialog = AlertDialog.Builder(requireContext()).create()
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
