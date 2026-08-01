package com.example.myapplication.ui.home.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatImageIncomingBinding

class IncomingImageViewHolder(
    private val binding: ItemChatImageIncomingBinding,
    private val onMessageLongClick: (View, Message) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
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

        if (message.content.isNotEmpty()) {
            Glide.with(binding.ivImageContent.context)
                .load(message.content)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.ivImageContent)
        }

        binding.root.setOnLongClickListener {
            onMessageLongClick(binding.ivImageContent, message)
            true
        }
    }
}
