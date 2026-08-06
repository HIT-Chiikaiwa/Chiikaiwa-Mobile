package com.example.myapplication.ui.home.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatBookingIncomingBinding
import com.example.myapplication.utils.BookingMessageHelper
import com.example.myapplication.utils.TimeUtils

class IncomingBookingViewHolder(
    private val binding: ItemChatBookingIncomingBinding,
    private val onMessageLongClick: (View, Message) -> Unit,
    private val onBookingAction: ((String, String) -> Unit)?,
    private val currentUserId: String = ""
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        val parsed = BookingMessageHelper.parseBookingJson(
            message.content,
            isOutgoing = false,
            senderName = message.sender.fullName ?: "",
            currentUserId = currentUserId
        )

        binding.tvBookingTitle.text = parsed.title
        binding.tvTime.text = TimeUtils.formatRelativeTime(message.createdAt)

        val status = parsed.status?.uppercase() ?: ""
        val isCreator = (parsed.creatorId != null && parsed.creatorId == currentUserId) || 
                        (message.sender.id == currentUserId && currentUserId.isNotEmpty())

        if (status == "PENDING") {
            if (isCreator) {
                binding.layoutActions.visibility = View.GONE
                binding.btnViewDetail.visibility = View.VISIBLE
                binding.tvBookingReason.visibility = View.GONE
            } else {
                binding.layoutActions.visibility = View.VISIBLE
                binding.btnViewDetail.visibility = View.GONE
                binding.tvBookingReason.visibility = View.GONE
            }
        } else if (status == "CANCELLED" || status == "REJECTED") {
            binding.layoutActions.visibility = View.GONE
            binding.btnViewDetail.visibility = View.GONE
            if (!parsed.reason.isNullOrEmpty()) {
                binding.tvBookingReason.text = "Lí do: ${parsed.reason}"
                binding.tvBookingReason.visibility = View.VISIBLE
            } else {
                binding.tvBookingReason.visibility = View.GONE
            }
        } else {
            binding.layoutActions.visibility = View.GONE
            binding.btnViewDetail.visibility = View.VISIBLE
            binding.tvBookingReason.visibility = View.GONE
        }

        val avatar = message.sender.avatar
        if (!avatar.isNullOrEmpty()) {
            Glide.with(binding.ivSenderAvatar.context)
                .load(avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivSenderAvatar)
        }

        binding.btnAccept.setOnClickListener {
            parsed.bookingId?.let { id -> onBookingAction?.invoke(id, "ACCEPT") }
        }

        binding.btnReject.setOnClickListener {
            parsed.bookingId?.let { id -> onBookingAction?.invoke(id, "REJECT") }
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
