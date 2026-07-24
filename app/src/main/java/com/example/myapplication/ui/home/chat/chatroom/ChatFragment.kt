package com.example.myapplication.ui.home.chat.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ActivityChatBinding
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.MessageAdapter
import com.example.myapplication.ui.home.chat.component.ReactionPopup
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by lazy {
        ViewModelProvider(this)[ChatViewModel::class.java]
    }
    private lateinit var adapter: MessageAdapter
    private var conversationId: String = ""
    private var targetUserId: String = ""
    private var replyToMessage: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: ""
        targetUserId = arguments?.getString(ARG_TARGET_USER_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (conversationId.isNotEmpty()) {
            viewModel.fetchMessages(conversationId)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(viewModel.currentUserId) { anchorView, message ->
            showReactionPopup(anchorView, message)
        }
        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.adapter = adapter
    }

    private fun showReactionPopup(anchorView: View, message: Message) {
        val popup = ReactionPopup(
            context = requireContext(),
            currentUserId = viewModel.currentUserId,
            onEmojiSelect = { msg, emoji -> viewModel.addReaction(msg.id, emoji) },
            onReplyClick = { msg ->
                replyToMessage = msg
                binding.etMessage.hint = "Trả lời ${msg.sender.fullName}: ${msg.content}"
                binding.etMessage.requestFocus()
            },
            onPinClick = { msg -> viewModel.pinMessage(msg.id) },
            onUnpinClick = { msg -> viewModel.unpinMessage(msg.id) },
            onRecallClick = { msg -> viewModel.recallMessage(msg.id) },
            onForwardClick = { msg ->
                Toast.makeText(requireContext(), "Chọn cuộc trò chuyện để chuyển tiếp", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { msg -> viewModel.deleteMessage(msg.id) }
        )
        popup.show(anchorView, message)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnUserAction.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            if (replyToMessage != null) {
                viewModel.replyMessage(replyToMessage!!.id, text)
                replyToMessage = null
                binding.etMessage.hint = "Nhập tin nhắn..."
            } else if (targetUserId.isNotEmpty()) {
                viewModel.sendRealtimeMessage(targetUserId, text)
            }
            binding.etMessage.setText("")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                adapter.submitList(state.data) {
                                    if (state.data.isNotEmpty()) {
                                        binding.rvChatMessages.scrollToPosition(state.data.size - 1)
                                    }
                                }
                            }
                            is UiState.Error -> {}
                            else -> {}
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
        private const val ARG_TARGET_USER_ID = "target_user_id"

        fun newInstance(conversationId: String, targetUserId: String = ""): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_TARGET_USER_ID, targetUserId)
                }
            }
        }
    }
}
