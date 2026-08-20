package com.example.myapplication.ui.home.chat.chatroom

import android.content.Context
import com.example.myapplication.R

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentChatBinding
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.adapter.MessageAdapter
import com.example.myapplication.ui.home.schedule.BookingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by lazy {
        ViewModelProvider(this)[ChatViewModel::class.java]
    }
    private val bookingViewModel: BookingViewModel by lazy {
        ViewModelProvider(this)[BookingViewModel::class.java]
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
        _binding = FragmentChatBinding.inflate(inflater, container, false)
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
        binding.btnCreateSchedule.isEnabled = false
        binding.btnCreateSchedule.alpha = 0.5f
        binding.btnMic.isEnabled = false
        binding.btnMic.alpha = 0.5f
        binding.btnFolder.isEnabled = false
        binding.btnFolder.alpha = 0.5f
        binding.btnSticker.isEnabled = false
        binding.btnSticker.alpha = 0.5f
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

    private fun openPartnerProfile(specifiedUserId: String? = null) {
        val idToOpen = when {
            !specifiedUserId.isNullOrEmpty() -> specifiedUserId
            targetUserId.isNotEmpty() -> targetUserId
            else -> {
                val messages = (viewModel.uiState.value as? UiState.Success)?.data ?: emptyList()
                messages.firstOrNull { it.sender.id != viewModel.currentUserId }?.sender?.id ?: ""
            }
        }
        if (idToOpen.isNotEmpty()) {
            val intent = Intent(requireContext(), com.example.myapplication.ui.profile.ProfileActivity::class.java).apply {
                putExtra("target_user_id", idToOpen)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            currentUserId = viewModel.currentUserId,
            partnerName = userName,
            onMessageLongClick = { anchorView, message ->
                ChatDialogManager.showActionPopup(
                    context = requireContext(),
                    anchorView = anchorView,
                    message = message,
                    currentUserId = viewModel.currentUserId,
                    viewModel = viewModel,
                    bookingViewModel = bookingViewModel,
                    conversationId = conversationId
                )
            },
            onBookingAction = { bookingId, action ->
                viewModel.performBookingAction(bookingId, action)
            },
            onAvatarClick = { senderId ->
                openPartnerProfile(senderId)
            }
        )
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val profileClickListener = View.OnClickListener {
            openPartnerProfile()
        }
        binding.tvChatTitle.setOnClickListener(profileClickListener)

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnCreateSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_chatFragment_to_createAppointmentFragment,
                androidx.core.os.bundleOf(
                    "conversation_id" to conversationId,
                    "target_user_name" to userName
                )
            )
        }

        binding.btnSticker.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show()
        }

        binding.btnMic.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show()
        }

        binding.btnFolder.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelected(uri: Uri) {
        val context = requireContext()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
                if (bitmap != null) {
                    val maxDimension = 1280
                    val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = Math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                        val width = Math.round(ratio * bitmap.width)
                        val height = Math.round(ratio * bitmap.height)
                        android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
                    } else bitmap

                    tempFile.outputStream().use { output ->
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, output)
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val mimeType = "image/jpeg"
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
                    viewModel.onlineStatus.collect { status ->
                        if (status != null) {
                            binding.layoutOnlineStatus.visibility = View.VISIBLE
                            if (status.isOnline) {
                                binding.viewOnlineIndicator.visibility = View.VISIBLE
                                binding.tvOnlineStatus.text = "Đang hoạt động"
                            } else {
                                binding.viewOnlineIndicator.visibility = View.GONE
                                if (!status.lastSeen.isNullOrBlank()) {
                                    val relativeTime = com.example.myapplication.utils.TimeUtils.formatRelativeTime(status.lastSeen)
                                    if (relativeTime == "Vừa xong" || relativeTime.contains("trước")) {
                                        binding.tvOnlineStatus.text = "Hoạt động $relativeTime"
                                    } else {
                                        binding.tvOnlineStatus.text = "Hoạt động từ $relativeTime"
                                    }
                                } else {
                                    binding.tvOnlineStatus.text = "Ngoại tuyến"
                                }
                            }
                        } else {
                            binding.layoutOnlineStatus.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.isChatDisabled.collect { isDisabled ->
                        if (isDisabled) {
                            disableMessagingInput()
                        }
                    }
                }

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

                launch {
                    bookingViewModel.event.collect { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    bookingViewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Error -> {
                                viewModel.fetchMessages()
                            }
                            is UiState.Success -> {
                                val b = state.data
                                val bId = b.id
                                val bStatus = b.status
                                if (!bId.isNullOrEmpty() && !bStatus.isNullOrEmpty()) {
                                    viewModel.updateBookingMessageStatus(bId, bStatus, b.cancelReason)
                                } else {
                                    viewModel.fetchMessages()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOnlineStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)
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
