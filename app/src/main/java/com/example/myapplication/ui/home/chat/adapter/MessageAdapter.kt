package com.example.myapplication.ui.home.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemChatIncomingBinding
import com.example.myapplication.databinding.ItemChatOutgoingBinding
import com.example.myapplication.ui.home.chat.adapter.viewholder.IncomingTextViewHolder
import com.example.myapplication.ui.home.chat.adapter.viewholder.OutgoingTextViewHolder

class MessageAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (View, Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.sender.id == currentUserId) TYPE_OUTGOING else TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_OUTGOING) {
            val binding = ItemChatOutgoingBinding.inflate(inflater, parent, false)
            OutgoingTextViewHolder(binding, onMessageLongClick)
        } else {
            val binding = ItemChatIncomingBinding.inflate(inflater, parent, false)
            IncomingTextViewHolder(binding, onMessageLongClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is OutgoingTextViewHolder) {
            holder.bind(message)
        } else if (holder is IncomingTextViewHolder) {
            holder.bind(message)
        }
    }

    companion object {
        private const val TYPE_INCOMING = 0
        private const val TYPE_OUTGOING = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                if (oldItem.id == newItem.id) return true
                if (oldItem.id.startsWith("temp_") && oldItem.content == newItem.content && oldItem.sender.id == newItem.sender.id) {
                    return true
                }
                return false
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }
}
