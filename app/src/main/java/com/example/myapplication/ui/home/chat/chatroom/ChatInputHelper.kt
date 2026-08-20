package com.example.myapplication.ui.home.chat.chatroom

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.example.myapplication.databinding.FragmentChatBinding

class ChatInputHelper(
    private val binding: FragmentChatBinding,
    private val onSendMessage: () -> Unit
) {
    fun setup() {
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasContent = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasContent) View.VISIBLE else View.GONE
                binding.btnMic.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnFolder.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnGallery.visibility = if (hasContent) View.GONE else View.VISIBLE
                binding.btnCreateSchedule.visibility = if (hasContent) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                onSendMessage()
                true
            } else {
                false
            }
        }
    }
}
