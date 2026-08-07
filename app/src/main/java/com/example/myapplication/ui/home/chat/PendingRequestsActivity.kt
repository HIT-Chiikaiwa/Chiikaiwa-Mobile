package com.example.myapplication.ui.home.chat

import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityPendingRequestsBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.home.chat.adapter.PendingRequestsAdapter

class PendingRequestsActivity : BaseActivity<ActivityPendingRequestsBinding>() {

    override fun inflateBinding() = ActivityPendingRequestsBinding.inflate(layoutInflater)

    private val viewModel: FriendsListViewModel by viewModels()
    private lateinit var pendingAdapter: PendingRequestsAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        pendingAdapter = PendingRequestsAdapter(
            onAccept = { item ->
                val reqId = item.requestId ?: return@PendingRequestsAdapter
                val targetUserId = item.userId
                viewModel.acceptFriendRequest(reqId) {
                    showToast("Đã đồng ý kết bạn với ${item.lastName ?: ""} ${item.firstName ?: ""}")
                    if (!targetUserId.isNullOrEmpty()) {
                        viewModel.startDirectChat(targetUserId) { _, _ ->
                            viewModel.fetchConversations()
                        }
                    }
                    loadData()
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

        binding.rvPendingRequests.layoutManager = LinearLayoutManager(this)
        binding.rvPendingRequests.adapter = pendingAdapter

        loadData()
    }

    private fun loadData() {
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

    override fun observeData() {
    }
}
