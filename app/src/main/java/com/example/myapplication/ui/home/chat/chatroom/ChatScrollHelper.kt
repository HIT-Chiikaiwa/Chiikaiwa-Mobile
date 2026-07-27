package com.example.myapplication.ui.home.chat.chatroom

import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ui.home.chat.adapter.MessageAdapter

class ChatScrollHelper(
    private val recyclerView: RecyclerView,
    private val adapterProvider: () -> MessageAdapter
) {
    fun scrollToBottom(delayMs: Long = 0) {
        val action = Runnable {
            val count = adapterProvider().itemCount
            if (count > 0) {
                recyclerView.scrollToPosition(count - 1)
            }
        }
        if (delayMs > 0) {
            recyclerView.postDelayed(action, delayMs)
        } else {
            recyclerView.post(action)
        }
    }
}
