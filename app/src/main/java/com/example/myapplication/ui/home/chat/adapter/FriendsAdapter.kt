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

            val lastMsgText = if (item.lastMessage != null) {
                if (item.lastMessage.isRecalled) "Tin nhắn đã thu hồi" else (item.lastMessage.content ?: "")
            } else {
                "Chưa có tin nhắn"
            }
            binding.tvLastMessage.text = lastMsgText

            binding.tvTime.text = item.lastMessage?.createdDate?.takeLast(5) ?: ""

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
