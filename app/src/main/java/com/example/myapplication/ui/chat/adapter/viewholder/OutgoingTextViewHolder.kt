package com.example.myapplication.ui.chat.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatOutgoingBinding
import com.example.myapplication.utils.TimeUtils

class OutgoingTextViewHolder(
    private val binding: ItemChatOutgoingBinding,
    private val onMessageLongClick: (View, Message) -> Unit,
    private val partnerName: String = "",
    private val onReactionClick: ((Message, String) -> Unit)? = null,
    private val currentUserId: String = ""
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        val displayContent = if (message.isRecalled) {
            "Tin nhắn đã được thu hồi"
        } else {
            com.example.myapplication.utils.BookingMessageHelper.formatIfBookingJson(
                message.content,
                isOutgoing = true,
                senderName = message.sender.fullName ?: "",
                partnerName = partnerName
            )
        }
        binding.tvMessageContent.text = displayContent
        binding.tvTime.text = TimeUtils.formatRelativeTime(message.createdAt)

        val avatar = message.sender.avatar
        if (!avatar.isNullOrEmpty()) {
            Glide.with(binding.ivMyAvatar.context)
                .load(avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivMyAvatar)
        } else {
            binding.ivMyAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        if (message.replyToMessage != null && !message.isRecalled) {
            binding.layoutReplyInBubble.visibility = View.VISIBLE
            binding.tvReplySender.text = message.replyToMessage.senderName ?: "Người dùng"
            binding.tvReplyContent.text = message.replyToMessage.content ?: ""
        } else {
            binding.layoutReplyInBubble.visibility = View.GONE
        }

        bindReactions(binding.layoutReactions, message.reactions, message)

        binding.layoutMessageBubble.setOnLongClickListener {
            onMessageLongClick(binding.layoutMessageBubble, message)
            true
        }
    }

    private fun bindReactions(
        layoutReactions: android.widget.LinearLayout,
        reactions: List<com.example.myapplication.data.model.Reaction>,
        message: Message
    ) {
        layoutReactions.removeAllViews()
        val context = layoutReactions.context
        if (reactions.isEmpty()) {
            layoutReactions.visibility = View.GONE
            return
        }
        layoutReactions.visibility = View.VISIBLE

        for (reaction in reactions) {
            if (reaction.count == null || reaction.count <= 0) continue

            val hasUserReacted = reaction.userIds.contains(currentUserId)

            val textView = android.widget.TextView(context).apply {
                text = "${reaction.emoji} ${reaction.count}"
                textSize = 10f 
                setTextColor(context.getColor(R.color.brown))

                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 20f
                    if (hasUserReacted) {
                        setColor(0x308D6E63) 
                        setStroke(1, 0xFF8D6E63.toInt()) 
                    } else {
                        setColor(0x10000000)
                        setStroke(1, 0x20000000)
                    }
                }
                background = drawable
                val padHorizontal = (8 * context.resources.displayMetrics.density).toInt()
                val padVertical = (4 * context.resources.displayMetrics.density).toInt()
                setPadding(padHorizontal, padVertical, padHorizontal, padVertical)
                
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins((6 * context.resources.displayMetrics.density).toInt(), 0, 0, 0)
                }
                layoutParams = params
                
                setOnClickListener {
                    reaction.emoji?.let { emoji ->
                        onReactionClick?.invoke(message, emoji)
                    }
                }
            }
            layoutReactions.addView(textView)
        }
    }
}
