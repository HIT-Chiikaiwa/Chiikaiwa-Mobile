package com.example.myapplication.ui.home.chat.adapter.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatIncomingBinding

class IncomingTextViewHolder(
    private val binding: ItemChatIncomingBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessageContent.text = message.content
        Glide.with(binding.ivSenderAvatar.context)
            .load(message.sender.avatar)
            .into(binding.ivSenderAvatar)
    }
}
