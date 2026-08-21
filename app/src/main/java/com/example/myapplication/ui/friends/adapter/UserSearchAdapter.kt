package com.example.myapplication.ui.friends.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.UserSearchDto
import com.example.myapplication.databinding.ItemUserSearchResultBinding

class UserSearchAdapter(
    private val onSendFriendRequest: (UserSearchDto) -> Unit,
    private val onStartChat: (UserSearchDto) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    private val userList = ArrayList<UserSearchDto>()

    fun submitList(list: List<UserSearchDto>) {
        userList.clear()
        userList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    inner class UserViewHolder(private val binding: ItemUserSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserSearchDto) {
            val fullName = "${user.lastName ?: ""} ${user.firstName ?: ""}".trim()
            binding.tvUserName.text = fullName.ifEmpty { "Người dùng" }
            binding.tvUserPhone.text = user.phone ?: "Không có SĐT"

            if (!user.avatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }

            when (user.friendshipStatus) {
                "FRIEND", "ACCEPTED" -> {
                    binding.btnAction.text = "Nhắn tin"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.alpha = 1.0f
                    binding.btnAction.setOnClickListener { onStartChat(user) }
                }
                "PENDING" -> {
                    binding.btnAction.text = "Đã gửi y/c"
                    binding.btnAction.isEnabled = false
                    binding.btnAction.alpha = 0.6f
                    binding.btnAction.setOnClickListener(null)
                }
                else -> {
                    binding.btnAction.text = "Kết bạn"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.alpha = 1.0f
                    binding.btnAction.setOnClickListener { onSendFriendRequest(user) }
                }
            }
        }
    }
}
