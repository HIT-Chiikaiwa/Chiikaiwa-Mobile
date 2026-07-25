package com.example.myapplication.ui.home.chat.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.myapplication.data.model.Message

class ReactionPopup(
    private val context: Context,
    private val currentUserId: String,
    private val onRecallClick: (Message) -> Unit,
    private val onDeleteClick: (Message) -> Unit
) {

    fun show(anchorView: View, message: Message) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            elevation = 16f
        }

        val popupWindow = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 20f
        }

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

        if (message.sender.id == currentUserId && !message.isRecalled) {
            addOption("🔄 Thu hồi tin nhắn", Color.parseColor("#E65100")) { onRecallClick(message) }
        }

        addOption("🗑️ Xóa phía tôi", Color.RED) { onDeleteClick(message) }

        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height - 200, Gravity.CENTER)
    }
}
