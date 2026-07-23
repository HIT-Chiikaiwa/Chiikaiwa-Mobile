package com.example.myapplication.ui.home.chat.adapter.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatOutgoingBinding

class OutgoingTextViewHolder(
    private val binding: ItemChatOutgoingBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.tvMessageContent.text = message.content
        Glide.with(binding.ivMyAvatar.context)
            .load(message.sender.avatar)
            .into(binding.ivMyAvatar)
    }
}
