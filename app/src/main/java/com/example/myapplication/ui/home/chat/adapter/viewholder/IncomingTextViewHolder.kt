package com.example.myapplication.ui.home.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatIncomingBinding

class IncomingTextViewHolder(
    private val binding: ItemChatIncomingBinding,
    private val onMessageLongClick: (View, Message) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessageContent.text = if (message.isRecalled) "Tin nhắn đã được thu hồi" else message.content

        val avatar = message.sender.avatar
        if (!avatar.isNullOrEmpty()) {
            Glide.with(binding.ivSenderAvatar.context)
                .load(avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivSenderAvatar)
        } else {
            binding.ivSenderAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        binding.root.setOnLongClickListener {
            onMessageLongClick(binding.tvMessageContent, message)
            true
        }
    }
}
