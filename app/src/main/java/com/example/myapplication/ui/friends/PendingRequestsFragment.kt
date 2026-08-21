package com.example.myapplication.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentPendingRequestsBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.friends.adapter.PendingRequestsAdapter

class PendingRequestsFragment : BaseFragment<FragmentPendingRequestsBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPendingRequestsBinding.inflate(inflater, container, false)

    private val friendRequestViewModel: FriendRequestViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()
    private lateinit var pendingAdapter: PendingRequestsAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        pendingAdapter = PendingRequestsAdapter(
            onAccept = { item ->
                val reqId = item.requestId ?: return@PendingRequestsAdapter
                val targetUserId = item.userId
                friendRequestViewModel.acceptFriendRequest(reqId) {
                    showToast("Đã đồng ý kết bạn với ${item.lastName ?: ""} ${item.firstName ?: ""}")
                    if (!targetUserId.isNullOrEmpty()) {
                        conversationViewModel.startDirectChat(targetUserId) { _, _ ->
                            conversationViewModel.fetchConversations()
                        }
                    }
                    loadData()
                }
            },
            onReject = { item ->
                val reqId = item.requestId ?: return@PendingRequestsAdapter
                friendRequestViewModel.rejectFriendRequest(reqId) {
                    showToast("Đã từ chối lời mời kết bạn")
                    loadData()
                }
            }
        )

        binding.rvPendingRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingRequests.adapter = pendingAdapter

        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        friendRequestViewModel.getPendingFriendRequests { list ->
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

    override fun observeData() {
        friendRequestViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }
    }
}
