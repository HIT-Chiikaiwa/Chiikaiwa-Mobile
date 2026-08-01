package com.example.myapplication.ui.home.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.databinding.ItemFriendBinding

class FriendsAdapter(
    private val onItemClick: (ConversationResponse) -> Unit
) : ListAdapter<ConversationResponse, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(private val binding: ItemFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConversationResponse) {
            val name = item.groupName ?: item.lastMessage?.senderName ?: "Người dùng"
            binding.tvFriendName.text = name

            val context = binding.root.context
            val currentUserId = com.example.myapplication.data.local.PreferenceManager(context).getUserId()

            val lastMsgText = if (item.lastMessage != null) {
                val msg = item.lastMessage
                if (msg.isRecalled) {
                    "Tin nhắn đã thu hồi"
                } else {
                    val isMe = msg.senderId == currentUserId
                    val prefix = if (isMe) "Bạn: " else if (!msg.senderName.isNullOrEmpty()) "${msg.senderName}: " else ""

                    val contentDescription = when (msg.messageType?.uppercase()) {
                        "IMAGE" -> "Đã gửi 1 hình ảnh"
                        "VIDEO" -> "Đã gửi 1 video"
                        "VOICE", "AUDIO" -> "Đã gửi 1 tin nhắn thoại"
                        "FILE" -> "Đã gửi 1 tệp tài liệu"
                        "LOCATION" -> "Đã chia sẻ 1 vị trí"
                        else -> msg.content ?: ""
                    }
                    "$prefix$contentDescription"
                }
            } else {
                "Chưa có tin nhắn"
            }
            binding.tvLastMessage.text = lastMsgText

            binding.tvTime.text = com.example.myapplication.utils.TimeUtils.formatChatTime(item.lastMessage?.createdDate)

            val avatarUrl = item.groupAvatar ?: item.lastMessage?.senderAvatar

            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivFriendAvatar)
            } else {
                binding.ivFriendAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ConversationResponse>() {
        override fun areItemsTheSame(oldItem: ConversationResponse, newItem: ConversationResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConversationResponse, newItem: ConversationResponse): Boolean {
            return oldItem == newItem
        }
    }
}
