package com.example.myapplication.ui.notification

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.databinding.ItemNotificationBinding
import com.example.myapplication.utils.TimeUtils

class NotificationAdapter(
    private val onItemClick: (NotificationDto) -> Unit,
    private val onItemLongClick: ((NotificationDto) -> Unit)? = null
) : ListAdapter<NotificationDto, NotificationAdapter.NotificationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationDto) {
            val actorName = "${item.actorLastName ?: ""} ${item.actorFirstName ?: ""}".trim()
            val textContent = if (actorName.isNotEmpty() && !item.content.orEmpty().contains(actorName)) {
                "$actorName: ${item.content ?: ""}"
            } else {
                item.content ?: ""
            }

            binding.tvContent.text = textContent

            // Format time formatted like messages (relative time / UTC to VN time)
            val timeText = TimeUtils.formatRelativeTime(item.createdDate)
            binding.tvTimeAgo.text = timeText

            // Visual unread indicator
            val isUnread = item.isRead == false
            binding.viewUnreadDot.visibility = if (isUnread) View.VISIBLE else View.GONE
            binding.tvContent.setTypeface(null, if (isUnread) Typeface.BOLD else Typeface.NORMAL)

            if (!item.actorAvatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.actorAvatar)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                onItemLongClick != null
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NotificationDto>() {
        override fun areItemsTheSame(oldItem: NotificationDto, newItem: NotificationDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationDto, newItem: NotificationDto): Boolean {
            return oldItem == newItem
        }
    }
}
