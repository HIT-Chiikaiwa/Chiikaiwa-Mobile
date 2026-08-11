package com.example.myapplication.ui.home.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.BlockedUserDto
import com.example.myapplication.databinding.ItemBlockedUserBinding

class BlockedUsersAdapter(
    private val onUnblockClick: (BlockedUserDto) -> Unit
) : RecyclerView.Adapter<BlockedUsersAdapter.BlockedUserViewHolder>() {

    private val blockedList = ArrayList<BlockedUserDto>()

    fun submitList(list: List<BlockedUserDto>) {
        blockedList.clear()
        blockedList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedUserViewHolder {
        val binding = ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedUserViewHolder, position: Int) {
        holder.bind(blockedList[position])
    }

    override fun getItemCount(): Int = blockedList.size

    inner class BlockedUserViewHolder(private val binding: ItemBlockedUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: BlockedUserDto) {
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

            binding.btnUnblock.setOnClickListener {
                onUnblockClick(user)
            }
        }
    }
}
