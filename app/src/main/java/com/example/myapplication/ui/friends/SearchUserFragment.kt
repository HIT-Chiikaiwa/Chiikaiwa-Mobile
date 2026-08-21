package com.example.myapplication.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSearchUserBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.friends.adapter.UserSearchAdapter

class SearchUserFragment : BaseFragment<FragmentSearchUserBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchUserBinding.inflate(inflater, container, false)

    private val friendRequestViewModel: FriendRequestViewModel by viewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()
    private lateinit var searchAdapter: UserSearchAdapter

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        searchAdapter = UserSearchAdapter(
            onSendFriendRequest = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                friendRequestViewModel.sendFriendRequest(targetId) {
                    showToast("Đã gửi yêu cầu kết bạn")
                }
            },
            onStartChat = { user ->
                val targetId = user.id ?: return@UserSearchAdapter
                conversationViewModel.startDirectChat(targetId) { convId, userName ->
                    val bundle = android.os.Bundle().apply {
                        putString("conversation_id", convId)
                        putString("target_user_id", targetId)
                        putString("user_name", userName)
                        putBoolean("is_disabled", false)
                    }
                    findNavController().navigate(R.id.chatFragment, bundle)
                }
            }
        )

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = searchAdapter

        val performSearch = {
            val kw = binding.edtKeyword.text.toString().trim()
            if (kw.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                friendRequestViewModel.searchUsers(kw) { results ->
                    binding.progressBar.visibility = View.GONE
                    searchAdapter.submitList(results)
                }
            }
        }

        binding.btnSearch.setOnClickListener { performSearch() }
        binding.edtKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    override fun observeData() {
        friendRequestViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }

        conversationViewModel.uiState.observeState { state ->
            if (state is UiState.Error) {
                showToast(state.message)
            }
        }
    }
}
