package com.example.myapplication.ui.home.chat.chatroom

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityChatBinding
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.MessageAdapter
import com.example.myapplication.ui.home.chat.component.ReactionPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatFragment : Fragment() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by lazy {
        ViewModelProvider(this)[ChatViewModel::class.java]
    }
    private lateinit var adapter: MessageAdapter
    private lateinit var inputHelper: ChatInputHelper
    private lateinit var scrollHelper: ChatScrollHelper

    private var conversationId: String = ""
    private var targetUserId: String = ""
    private var userName: String = ""
    private var isDisabled: Boolean = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: ""
        targetUserId = arguments?.getString(ARG_TARGET_USER_ID) ?: ""
        userName = arguments?.getString(ARG_USER_NAME) ?: ""
        isDisabled = arguments?.getBoolean(ARG_IS_DISABLED, false) ?: false
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
        if (userName.isNotEmpty()) {
            binding.tvChatTitle.text = userName
        }
        setupHelpers()
        setupWindowInsets()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (isDisabled) {
            disableMessagingInput()
        }

        viewModel.initChatSession(conversationId, targetUserId)
    }

    private fun disableMessagingInput() {
        binding.etMessage.isEnabled = false
        binding.etMessage.hint = "Không thể gửi tin nhắn"
        binding.btnSend.isEnabled = false
        binding.btnSend.alpha = 0.5f
        binding.btnGallery.isEnabled = false
        binding.btnGallery.alpha = 0.5f
    }

    private fun setupHelpers() {
        scrollHelper = ChatScrollHelper(binding.rvChatMessages) { adapter }
        inputHelper = ChatInputHelper(binding) { sendMessage() }
        inputHelper.setup()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBarsHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPadding = if (imeHeight > 0) imeHeight else navigationBarsHeight
            binding.root.setPadding(0, 0, 0, bottomPadding)

            if (imeHeight > 0 && ::adapter.isInitialized) {
                scrollHelper.scrollToBottom()
            }
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(viewModel.currentUserId) { anchorView, message ->
            showActionPopup(anchorView, message)
        }
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = adapter
    }

    private fun showActionPopup(anchorView: View, message: com.example.myapplication.data.model.Message) {
        val popup = ReactionPopup(
            context = requireContext(),
            currentUserId = viewModel.currentUserId,
            onRecallClick = { msg -> viewModel.recallMessage(msg.id) },
            onDeleteClick = { msg -> viewModel.deleteMessage(msg.id) }
        )
        popup.show(anchorView, message)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnGallery.setOnClickListener {
            openImagePicker()
        }

        binding.btnCreateSchedule.setOnClickListener {
            val intent = Intent(requireContext(), com.example.myapplication.ui.home.schedule.CreateAppointmentActivity::class.java).apply {
                putExtra("conversation_id", conversationId)
                putExtra("target_user_name", userName)
            }
            startActivity(intent)
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun handleImageSelected(uri: Uri) {
        val context = requireContext()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                withContext(Dispatchers.Main) {
                    viewModel.sendImageMessage(part, tempFile.absolutePath)
                    scrollHelper.scrollToBottom(delayMs = 100)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi chọn ảnh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            val destinationId = if (targetUserId.isNotEmpty()) targetUserId else conversationId
            viewModel.sendRealtimeMessage(destinationId, text)
            binding.etMessage.setText("")
            scrollHelper.scrollToBottom(delayMs = 50)
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
                                    scrollHelper.scrollToBottom()
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
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_IS_DISABLED = "is_disabled"

        fun newInstance(
            conversationId: String,
            targetUserId: String = "",
            userName: String = "",
            isDisabled: Boolean = false
        ): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_TARGET_USER_ID, targetUserId)
                    putString(ARG_USER_NAME, userName)
                    putBoolean(ARG_IS_DISABLED, isDisabled)
                }
            }
        }
    }
}
