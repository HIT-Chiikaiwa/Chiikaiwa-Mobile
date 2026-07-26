package com.example.myapplication.ui.home.chat.chatroom

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ChatFragment : Fragment() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by lazy {
        ViewModelProvider(this)[ChatViewModel::class.java]
    }
    private lateinit var adapter: MessageAdapter
    private var conversationId: String = ""
    private var targetUserId: String = ""
    private var userName: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageSelected(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickImageLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Cần cấp quyền truy cập ảnh", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID) ?: ""
        targetUserId = arguments?.getString(ARG_TARGET_USER_ID) ?: ""
        userName = arguments?.getString(ARG_USER_NAME) ?: ""
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
        setupWindowInsets()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.initChatSession(conversationId, targetUserId)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBarsHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPadding = if (imeHeight > 0) imeHeight else navigationBarsHeight
            binding.root.setPadding(0, 0, 0, bottomPadding)

            if (imeHeight > 0 && ::adapter.isInitialized && adapter.itemCount > 0) {
                binding.rvChatMessages.post {
                    binding.rvChatMessages.scrollToPosition(adapter.itemCount - 1)
                }
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

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasContent = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasContent) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnFolder.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnGallery.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnUserAction.visibility = if (hasContent) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun openImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun handleImageSelected(uri: Uri) {
        val context = requireContext()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)

        viewModel.sendImageMessage(part)

        binding.rvChatMessages.postDelayed({
            if (::adapter.isInitialized && adapter.itemCount > 0) {
                binding.rvChatMessages.scrollToPosition(adapter.itemCount - 1)
            }
        }, 100)
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            val destinationId = if (targetUserId.isNotEmpty()) targetUserId else conversationId
            viewModel.sendRealtimeMessage(destinationId, text)
            binding.etMessage.setText("")

            binding.rvChatMessages.postDelayed({
                if (::adapter.isInitialized && adapter.itemCount > 0) {
                    binding.rvChatMessages.scrollToPosition(adapter.itemCount - 1)
                }
            }, 50)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                val list = state.data
                                adapter.submitList(list) {
                                    binding.rvChatMessages.post {
                                        if (adapter.itemCount > 0) {
                                            binding.rvChatMessages.scrollToPosition(adapter.itemCount - 1)
                                        }
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
        private const val ARG_USER_NAME = "user_name"

        fun newInstance(conversationId: String, targetUserId: String = "", userName: String = ""): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_TARGET_USER_ID, targetUserId)
                    putString(ARG_USER_NAME, userName)
                }
            }
        }
    }
}
