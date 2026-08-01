package com.example.myapplication.ui.home.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.databinding.ItemChatImageIncomingBinding
import com.example.myapplication.databinding.ItemChatImageOutgoingBinding
import com.example.myapplication.databinding.ItemChatIncomingBinding
import com.example.myapplication.databinding.ItemChatOutgoingBinding
import com.example.myapplication.ui.home.chat.adapter.viewholder.IncomingImageViewHolder
import com.example.myapplication.ui.home.chat.adapter.viewholder.IncomingTextViewHolder
import com.example.myapplication.ui.home.chat.adapter.viewholder.OutgoingImageViewHolder
import com.example.myapplication.ui.home.chat.adapter.viewholder.OutgoingTextViewHolder

class MessageAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (View, Message) -> Unit
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
        val isOutgoing = message.sender.id == currentUserId
        return when {
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
                OutgoingTextViewHolder(binding, onMessageLongClick)
            }
            TYPE_INCOMING_TEXT -> {
                val binding = ItemChatIncomingBinding.inflate(inflater, parent, false)
                IncomingTextViewHolder(binding, onMessageLongClick)
            }
            TYPE_OUTGOING_IMAGE -> {
                val binding = ItemChatImageOutgoingBinding.inflate(inflater, parent, false)
                OutgoingImageViewHolder(binding, onMessageLongClick)
            }
            else -> {
                val binding = ItemChatImageIncomingBinding.inflate(inflater, parent, false)
                IncomingImageViewHolder(binding, onMessageLongClick)
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
        }
    }

    companion object {
        private const val TYPE_INCOMING_TEXT = 0
        private const val TYPE_OUTGOING_TEXT = 1
        private const val TYPE_OUTGOING_IMAGE = 2
        private const val TYPE_INCOMING_IMAGE = 3
    }
}
