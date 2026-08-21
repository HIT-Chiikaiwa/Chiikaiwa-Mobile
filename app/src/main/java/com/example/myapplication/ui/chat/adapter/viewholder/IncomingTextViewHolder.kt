package com.example.myapplication.ui.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatIncomingBinding
import com.example.myapplication.utils.TimeUtils

class IncomingTextViewHolder(
    private val binding: ItemChatIncomingBinding,
    private val onMessageLongClick: (View, Message) -> Unit,
    private val partnerName: String = "",
    private val onAvatarClick: ((String) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        val displayContent = if (message.isRecalled) {
            "Tin nhắn đã được thu hồi"
        } else {
            com.example.myapplication.utils.BookingMessageHelper.formatIfBookingJson(
                message.content,
                isOutgoing = false,
                senderName = message.sender.fullName ?: "",
                partnerName = partnerName
            )
        }
        binding.tvMessageContent.text = displayContent
        binding.tvTime.text = TimeUtils.formatRelativeTime(message.createdAt)

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

        binding.ivSenderAvatar.setOnClickListener {
            if (message.sender.id.isNotEmpty()) {
                onAvatarClick?.invoke(message.sender.id)
            }
        }

        binding.root.setOnLongClickListener {
            onMessageLongClick(binding.tvMessageContent, message)
            true
        }
    }
}
