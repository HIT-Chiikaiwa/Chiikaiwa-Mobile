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

        private fun translateNotificationContent(content: String): String {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return ""

            var result = trimmed

            if (result.contains("sent you a friend request", ignoreCase = true)) {
                result = result.replace(Regex("sent you a friend request\\.??!*$", RegexOption.IGNORE_CASE), "đã gửi cho bạn một lời mời kết bạn!")
            } else if (result.contains("accepted your friend request", ignoreCase = true)) {
                result = result.replace(Regex("accepted your friend request\\.??!*$", RegexOption.IGNORE_CASE), "đã chấp nhận lời mời kết bạn của bạn!")
            }
            
            else if (result.contains("invited you to a meeting", ignoreCase = true)) {
                result = result.replace(Regex("invited you to a meeting\\.?$", RegexOption.IGNORE_CASE), "đã mời bạn tham gia một cuộc hẹn.")
            } else if (result.contains("accepted your meeting", ignoreCase = true)) {
                result = result.replace(Regex("accepted your meeting\\.?$", RegexOption.IGNORE_CASE), "đã chấp nhận lịch hẹn của bạn.")
            } else if (result.contains("rejected your meeting", ignoreCase = true)) {
                result = result.replace(Regex("rejected your meeting\\.?$", RegexOption.IGNORE_CASE), "đã từ chối lịch hẹn của bạn.")
            } else if (result.contains("cancelled the meeting", ignoreCase = true)) {
                result = result.replace(Regex("cancelled the meeting\\.?$", RegexOption.IGNORE_CASE), "đã hủy lịch hẹn.")
            } else if (result.contains("completed the meeting", ignoreCase = true)) {
                result = result.replace(Regex("completed the meeting\\.?$", RegexOption.IGNORE_CASE), "đã hoàn thành cuộc hẹn.")
            } else if (result.contains("You have a meeting with", ignoreCase = true)) {
                result = result.replace("You have a meeting with", "Bạn có một cuộc hẹn với", ignoreCase = true)
                result = result.replace(" at ", " lúc ", ignoreCase = true)
            }
            
            else if (result.contains("has scheduled an appointment at", ignoreCase = true)) {
                result = result.replace(Regex("has scheduled an appointment at", RegexOption.IGNORE_CASE), "đã đặt một lịch hẹn vào lúc")
            } else if (result.contains("has scheduled a booking at", ignoreCase = true)) {
                result = result.replace(Regex("has scheduled a booking at", RegexOption.IGNORE_CASE), "đã đặt một lịch hẹn vào lúc")
            } else if (result.contains("has cancelled the appointment at", ignoreCase = true)) {
                result = result.replace(Regex("has cancelled the appointment at", RegexOption.IGNORE_CASE), "đã hủy lịch hẹn vào lúc")
            } else if (result.contains("has cancelled the booking at", ignoreCase = true)) {
                result = result.replace(Regex("has cancelled the booking at", RegexOption.IGNORE_CASE), "đã hủy lịch hẹn vào lúc")
            } else if (result.contains("has accepted the appointment at", ignoreCase = true)) {
                result = result.replace(Regex("has accepted the appointment at", RegexOption.IGNORE_CASE), "đã chấp nhận lịch hẹn vào lúc")
            } else if (result.contains("has accepted the booking at", ignoreCase = true)) {
                result = result.replace(Regex("has accepted the booking at", RegexOption.IGNORE_CASE), "đã chấp nhận lịch hẹn vào lúc")
            } else if (result.contains("has rejected the appointment at", ignoreCase = true)) {
                result = result.replace(Regex("has rejected the appointment at", RegexOption.IGNORE_CASE), "đã từ chối lịch hẹn vào lúc")
            } else if (result.contains("has rejected the booking at", ignoreCase = true)) {
                result = result.replace(Regex("has rejected the booking at", RegexOption.IGNORE_CASE), "đã từ chối lịch hẹn vào lúc")
            } else if (result.contains("has completed the appointment at", ignoreCase = true)) {
                result = result.replace(Regex("has completed the appointment at", RegexOption.IGNORE_CASE), "đã hoàn thành cuộc hẹn vào lúc")
            } else if (result.contains("has completed the booking at", ignoreCase = true)) {
                result = result.replace(Regex("has completed the booking at", RegexOption.IGNORE_CASE), "đã hoàn thành cuộc hẹn vào lúc")
            }

            return result
        }

        private fun isVietnamese(text: String): Boolean {
            val vietnameseChars = "àáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ"
            return vietnameseChars.any { text.contains(it, ignoreCase = true) }
        }

        fun bind(item: NotificationDto) {
            val actorName = "${item.actorLastName ?: ""} ${item.actorFirstName ?: ""}".trim()
            val translatedContent = translateNotificationContent(item.content ?: "")
            val rawContent = if (actorName.isNotEmpty() && !translatedContent.contains(actorName)) {
                val isAction = translatedContent.startsWith("đã") || translatedContent.startsWith("gửi") || translatedContent.startsWith("yêu cầu")
                if (isAction) {
                    "$actorName $translatedContent"
                } else {
                    "$actorName: $translatedContent"
                }
            } else {
                translatedContent
            }

            val textContent = TimeUtils.formatTimeInText(rawContent)
            binding.tvContent.text = textContent

            val timeText = TimeUtils.formatRelativeTime(item.createdDate)
            binding.tvTimeAgo.text = timeText

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
