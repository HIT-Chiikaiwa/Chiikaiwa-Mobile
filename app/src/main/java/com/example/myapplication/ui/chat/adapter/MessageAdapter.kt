package com.example.myapplication.ui.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.databinding.ItemChatBookingIncomingBinding
import com.example.myapplication.databinding.ItemChatBookingOutgoingBinding
import com.example.myapplication.databinding.ItemChatImageIncomingBinding
import com.example.myapplication.databinding.ItemChatImageOutgoingBinding
import com.example.myapplication.databinding.ItemChatIncomingBinding
import com.example.myapplication.databinding.ItemChatOutgoingBinding
import com.example.myapplication.ui.chat.adapter.viewholder.IncomingBookingViewHolder
import com.example.myapplication.ui.chat.adapter.viewholder.IncomingImageViewHolder
import com.example.myapplication.ui.chat.adapter.viewholder.IncomingTextViewHolder
import com.example.myapplication.ui.chat.adapter.viewholder.OutgoingBookingViewHolder
import com.example.myapplication.ui.chat.adapter.viewholder.OutgoingImageViewHolder
import com.example.myapplication.ui.chat.adapter.viewholder.OutgoingTextViewHolder
import com.example.myapplication.utils.BookingMessageHelper

class MessageAdapter(
    private val currentUserId: String,
    private val partnerName: String = "",
    private val onMessageLongClick: (View, Message) -> Unit,
    private val onBookingAction: ((String, String) -> Unit)? = null,
    private val onAvatarClick: ((String) -> Unit)? = null,
    private val onReactionClick: ((Message, String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    fun submitList(newList: List<Message>, onComplete: (() -> Unit)? = null) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
        onComplete?.invoke()
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val isBooking = message.type == MessageType.BOOKING || BookingMessageHelper.isBookingMessage(message.content)
        val creatorId = if (isBooking) {
            try {
                com.google.gson.Gson().fromJson(message.content, com.google.gson.JsonObject::class.java)
                    ?.get("creatorId")?.takeIf { !it.isJsonNull }?.asString
            } catch (e: Exception) { null }
        } else null

        val isOutgoing = (message.sender.id == currentUserId && currentUserId.isNotEmpty()) ||
                         (!creatorId.isNullOrEmpty() && creatorId == currentUserId)

        return when {
            isOutgoing && isBooking -> TYPE_OUTGOING_BOOKING
            !isOutgoing && isBooking -> TYPE_INCOMING_BOOKING
            isOutgoing && message.type == MessageType.IMAGE -> TYPE_OUTGOING_IMAGE
            !isOutgoing && message.type == MessageType.IMAGE -> TYPE_INCOMING_IMAGE
            isOutgoing -> TYPE_OUTGOING_TEXT
            else -> TYPE_INCOMING_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_OUTGOING_TEXT -> {
                val binding = ItemChatOutgoingBinding.inflate(inflater, parent, false)
                OutgoingTextViewHolder(binding, onMessageLongClick, partnerName, onReactionClick, currentUserId)
            }
            TYPE_INCOMING_TEXT -> {
                val binding = ItemChatIncomingBinding.inflate(inflater, parent, false)
                IncomingTextViewHolder(binding, onMessageLongClick, partnerName, onAvatarClick, onReactionClick, currentUserId)
            }
            TYPE_OUTGOING_IMAGE -> {
                val binding = ItemChatImageOutgoingBinding.inflate(inflater, parent, false)
                OutgoingImageViewHolder(binding, onMessageLongClick)
            }
            TYPE_INCOMING_IMAGE -> {
                val binding = ItemChatImageIncomingBinding.inflate(inflater, parent, false)
                IncomingImageViewHolder(binding, onMessageLongClick, onAvatarClick)
            }
            TYPE_OUTGOING_BOOKING -> {
                val binding = ItemChatBookingOutgoingBinding.inflate(inflater, parent, false)
                OutgoingBookingViewHolder(binding, onMessageLongClick, currentUserId)
            }
            TYPE_INCOMING_BOOKING -> {
                val binding = ItemChatBookingIncomingBinding.inflate(inflater, parent, false)
                IncomingBookingViewHolder(binding, onMessageLongClick, onBookingAction, currentUserId, onAvatarClick)
            }
            else -> {
                val binding = ItemChatIncomingBinding.inflate(inflater, parent, false)
                IncomingTextViewHolder(binding, onMessageLongClick, partnerName, onAvatarClick, onReactionClick, currentUserId)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is OutgoingTextViewHolder -> holder.bind(message)
            is IncomingTextViewHolder -> holder.bind(message)
            is OutgoingImageViewHolder -> holder.bind(message)
            is IncomingImageViewHolder -> holder.bind(message)
            is OutgoingBookingViewHolder -> holder.bind(message)
            is IncomingBookingViewHolder -> holder.bind(message)
        }
    }

    companion object {
        private const val TYPE_INCOMING_TEXT = 0
        private const val TYPE_OUTGOING_TEXT = 1
        private const val TYPE_OUTGOING_IMAGE = 2
        private const val TYPE_INCOMING_IMAGE = 3
        private const val TYPE_OUTGOING_BOOKING = 4
        private const val TYPE_INCOMING_BOOKING = 5
    }
}
