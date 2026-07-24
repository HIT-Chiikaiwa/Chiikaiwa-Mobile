package com.example.myapplication.ui.home.chat.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.myapplication.data.model.Message

class ReactionPopup(
    private val context: Context,
    private val currentUserId: String,
    private val onEmojiSelect: (Message, String) -> Unit,
    private val onReplyClick: (Message) -> Unit,
    private val onPinClick: (Message) -> Unit,
    private val onUnpinClick: (Message) -> Unit,
    private val onRecallClick: (Message) -> Unit,
    private val onForwardClick: (Message) -> Unit,
    private val onDeleteClick: (Message) -> Unit
) {

    fun show(anchorView: View, message: Message) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            elevation = 16f
        }

        // Emoji row
        val emojiRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 24)
        }

        val emojis = listOf("❤️", "👍", "😆", "😮", "😢", "😡")
        val popupWindow = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 20f
        }

        for (emoji in emojis) {
            val emojiTv = TextView(context).apply {
                text = emoji
                textSize = 24f
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    onEmojiSelect(message, emoji)
                    popupWindow.dismiss()
                }
            }
            emojiRow.addView(emojiTv)
        }
        container.addView(emojiRow)

        // Action menu options
        fun addOption(title: String, textColor: Int = Color.BLACK, onClick: () -> Unit) {
            val optionTv = TextView(context).apply {
                text = title
                textSize = 16f
                setTextColor(textColor)
                setPadding(16, 20, 16, 20)
                setOnClickListener {
                    onClick()
                    popupWindow.dismiss()
                }
            }
            container.addView(optionTv)
        }

        addOption("💬 Trả lời tin nhắn") { onReplyClick(message) }

        if (message.isPinned) {
            addOption("📌 Bỏ ghim tin nhắn") { onUnpinClick(message) }
        } else {
            addOption("📌 Ghim tin nhắn") { onPinClick(message) }
        }

        addOption("↩️ Chuyển tiếp tin nhắn") { onForwardClick(message) }

        if (message.sender.id == currentUserId && !message.isRecalled) {
            addOption("🔄 Thu hồi tin nhắn", Color.parseColor("#E65100")) { onRecallClick(message) }
        }

        addOption("🗑️ Xóa phía tôi", Color.RED) { onDeleteClick(message) }

        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height - 200, Gravity.CENTER)
    }
}
