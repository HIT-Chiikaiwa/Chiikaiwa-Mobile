package com.example.myapplication.ui.friends.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.FriendDto
import com.example.myapplication.databinding.ItemPendingRequestBinding

class PendingRequestsAdapter(
    private val onAccept: (FriendDto) -> Unit,
    private val onReject: (FriendDto) -> Unit
) : RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder>() {

    private val list = ArrayList<FriendDto>()

    fun submitList(newList: List<FriendDto>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(private val binding: ItemPendingRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendDto) {
            val fullName = "${item.lastName ?: ""} ${item.firstName ?: ""}".trim()
            binding.tvUserName.text = fullName.ifEmpty { "Người dùng" }
            binding.tvUserPhone.text = item.phone ?: "Đã gửi lời mời kết bạn"

            if (!item.avatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.avatar)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }

            binding.btnAccept.setOnClickListener { onAccept(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
        }
    }
}
