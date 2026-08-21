package com.example.myapplication.ui.friends.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.databinding.ItemFriendBinding

class FriendsAdapter(
    private val onItemClick: (ConversationResponse) -> Unit,
    private val onMoreClick: ((ConversationResponse, View) -> Unit)? = null
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
            val context = binding.root.context
            val currentUserId = com.example.myapplication.data.local.PreferenceManager(context).getUserId()

            val isUserUnavailable = item.memberCount == 1 || item.hasLeft

            val name = when {
                isUserUnavailable -> "Người dùng không tồn tại"
                !item.groupName.isNullOrEmpty() -> item.groupName
                item.lastMessage != null && item.lastMessage.senderId != currentUserId && !item.lastMessage.senderName.isNullOrEmpty() -> item.lastMessage.senderName
                else -> "Người dùng"
            }
            binding.tvFriendName.text = name

            val lastMsgText = if (item.lastMessage != null) {
                val msg = item.lastMessage
                if (msg.isRecalled) {
                    "Tin nhắn đã thu hồi"
                } else {
                    val isMe = msg.senderId == currentUserId
                    val prefix = if (isMe) "Bạn: " else if (!msg.senderName.isNullOrEmpty()) "${msg.senderName}: " else ""

                    val contentDescription = when {
                        msg.messageType?.uppercase() == "IMAGE" -> "Đã gửi 1 hình ảnh"
                        msg.messageType?.uppercase() == "VIDEO" -> "Đã gửi 1 video"
                        msg.messageType?.uppercase() == "VOICE" || msg.messageType?.uppercase() == "AUDIO" -> "Đã gửi 1 tin nhắn thoại"
                        msg.messageType?.uppercase() == "FILE" -> "Đã gửi 1 tệp tài liệu"
                        msg.messageType?.uppercase() == "LOCATION" -> "Đã chia sẻ 1 vị trí"
                        msg.messageType?.uppercase() == "BOOKING" || com.example.myapplication.utils.BookingJsonParser.isBookingMessage(msg.content) -> {
                            com.example.myapplication.utils.BookingJsonParser.formatBookingSummary(msg.content)
                        }
                        else -> msg.content ?: ""
                    }
                    "$prefix$contentDescription"
                }
            } else {
                "Chưa có tin nhắn"
            }
            binding.tvLastMessage.text = lastMsgText

            binding.tvTime.text = com.example.myapplication.utils.TimeUtils.formatRelativeTime(item.lastMessage?.createdDate)

            val avatarUrl = if (isUserUnavailable) null else (item.groupAvatar ?: if (item.lastMessage?.senderId != currentUserId) item.lastMessage?.senderAvatar else null)

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

            binding.btnMore.setOnClickListener { v ->
                onMoreClick?.invoke(item, v)
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
