package com.example.myapplication.ui.home.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatBookingOutgoingBinding
import com.example.myapplication.utils.BookingMessageHelper
import com.example.myapplication.utils.TimeUtils

class OutgoingBookingViewHolder(
    private val binding: ItemChatBookingOutgoingBinding,
    private val onMessageLongClick: (View, Message) -> Unit,
    private val currentUserId: String = ""
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        val parsed = BookingMessageHelper.parseBookingJson(
            message.content,
            isOutgoing = true,
            currentUserId = currentUserId
        )

        binding.tvBookingTitle.text = parsed.title
        binding.tvTime.text = TimeUtils.formatRelativeTime(message.createdAt)

        val status = parsed.status?.uppercase() ?: ""
        if (status == "CANCELLED" || status == "REJECTED") {
            binding.btnViewDetail.visibility = View.GONE
            if (!parsed.reason.isNullOrEmpty()) {
                binding.tvBookingReason.text = "Lí do: ${parsed.reason}"
                binding.tvBookingReason.visibility = View.VISIBLE
            } else {
                binding.tvBookingReason.visibility = View.GONE
            }
        } else {
            binding.btnViewDetail.visibility = View.VISIBLE
            binding.tvBookingReason.visibility = View.GONE
        }

        val bookingAvatar = parsed.avatarUrl
        if (!bookingAvatar.isNullOrEmpty()) {
            Glide.with(binding.ivBookingAvatar.context)
                .load(bookingAvatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivBookingAvatar)
        }

        val avatar = message.sender.avatar
        if (!avatar.isNullOrEmpty()) {
            Glide.with(binding.ivMyAvatar.context)
                .load(avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivMyAvatar)
        }

        binding.btnViewDetail.setOnClickListener {
            onMessageLongClick(binding.btnViewDetail, message)
        }

        binding.root.setOnLongClickListener {
            onMessageLongClick(binding.tvBookingTitle, message)
            true
        }
    }
}
