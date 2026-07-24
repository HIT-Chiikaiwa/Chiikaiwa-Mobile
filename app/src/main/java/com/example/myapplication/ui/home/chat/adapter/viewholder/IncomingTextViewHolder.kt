package com.example.myapplication.ui.home.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatIncomingBinding

class IncomingTextViewHolder(
    private val binding: ItemChatIncomingBinding,
    private val onMessageLongClick: (View, Message) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessageContent.text = if (message.isRecalled) "Tin nhắn đã được thu hồi" else message.content
        Glide.with(binding.ivSenderAvatar.context)
            .load(message.sender.avatar)
            .into(binding.ivSenderAvatar)

        binding.root.setOnLongClickListener {
            onMessageLongClick(binding.tvMessageContent, message)
            true
        }
    }
}
