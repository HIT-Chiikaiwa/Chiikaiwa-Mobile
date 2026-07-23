package com.example.myapplication.ui.home.chat.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.ActivityFriendsListBinding
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import kotlinx.coroutines.launch

class GroupDetailFragment : Fragment() {

    private var _binding: ActivityFriendsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by lazy {
        ViewModelProvider(this)[GroupViewModel::class.java]
    }

    private var conversationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFriendsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    fun updateGroupName(newName: String, avatarUrl: String? = null) {
        if (conversationId.isNotEmpty()) {
            viewModel.updateGroupInfo(conversationId, newName, avatarUrl)
        }
    }

    fun addMembers(memberIds: List<String>) {
        if (conversationId.isNotEmpty()) {
            viewModel.addMembers(conversationId, memberIds)
        }
    }

    fun removeMember(userId: String) {
        if (conversationId.isNotEmpty()) {
            viewModel.removeMember(conversationId, userId)
        }
    }

    fun dissolveGroup() {
        if (conversationId.isNotEmpty()) {
            viewModel.dissolveGroup(conversationId)
        }
    }

    fun transferOwnership(newOwnerId: String) {
        if (conversationId.isNotEmpty()) {
            viewModel.transferOwnership(conversationId, newOwnerId)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {}
                            is UiState.Success -> {}
                            is UiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            is UiState.Idle -> {}
                        }
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"

        fun newInstance(conversationId: String): GroupDetailFragment {
            return GroupDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                }
            }
        }
    }
}
