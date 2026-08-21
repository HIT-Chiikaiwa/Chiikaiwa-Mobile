package com.example.myapplication.ui.chat.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.LayoutReactionPopupBinding

class ReactionPopup(
    private val context: Context,
    private val currentUserId: String,
    private val onRecallClick: (Message) -> Unit,
    private val onDeleteClick: (Message) -> Unit
) {

    fun show(anchorView: View, message: Message) {
        val binding = LayoutReactionPopupBinding.inflate(LayoutInflater.from(context))

        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 20f
        }

        val isMyMessage = message.sender.id == currentUserId
        if (isMyMessage && !message.isRecalled) {
            binding.tvRecall.visibility = View.VISIBLE
            binding.divider.visibility = View.VISIBLE
            binding.tvRecall.setOnClickListener {
                onRecallClick(message)
                popupWindow.dismiss()
            }
        } else {
            binding.tvRecall.visibility = View.GONE
            binding.divider.visibility = View.GONE
        }

        binding.tvDelete.setOnClickListener {
            onDeleteClick(message)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height - 200, Gravity.CENTER)
    }
}
