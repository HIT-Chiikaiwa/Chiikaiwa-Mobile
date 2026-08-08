package com.example.myapplication.ui.notification

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.databinding.ActivityNotificationBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.FriendsListActivity
import com.example.myapplication.ui.home.schedule.ScheduleActivity
import com.example.myapplication.ui.profile.ProfileActivity

class NotificationActivity : BaseActivity<ActivityNotificationBinding>() {

    override fun inflateBinding() = ActivityNotificationBinding.inflate(layoutInflater)

    private val viewModel: NotificationViewModel by viewModels()

    private val recentAdapter by lazy {
        NotificationAdapter(
            onItemClick = { notification -> handleNotificationClick(notification) },
            onItemLongClick = { notification -> showNotificationOptionsDialog(notification) }
        )
    }

    private val earlierAdapter by lazy {
        NotificationAdapter(
            onItemClick = { notification -> handleNotificationClick(notification) },
            onItemLongClick = { notification -> showNotificationOptionsDialog(notification) }
        )
    }

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
        }

        binding.rvRecentNotifications.adapter = recentAdapter
        binding.rvEarlierNotifications.adapter = earlierAdapter

        checkAndShowReminderDialog(intent)
    }

    private fun checkAndShowReminderDialog(intent: Intent?) {
        if (intent?.getBooleanExtra("show_reminder_dialog", false) == true) {
            val title = intent.getStringExtra("reminder_title") ?: "Cuộc hẹn"
            val scheduledAt = intent.getStringExtra("reminder_scheduled_at") ?: ""
            com.example.myapplication.ui.home.schedule.AppointmentReminderDialog(this, title, scheduledAt).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotifications()
    }

    override fun observeData() {
        viewModel.notificationsState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.scrollViewContent.visibility = View.GONE
                        recentAdapter.submitList(emptyList())
                        earlierAdapter.submitList(emptyList())
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.scrollViewContent.visibility = View.VISIBLE

                        val recentList = if (list.size > 5) list.subList(0, 5) else list
                        val earlierList = if (list.size > 5) list.subList(5, list.size) else emptyList()

                        binding.tvRecentHeader.visibility = if (recentList.isNotEmpty()) View.VISIBLE else View.GONE
                        recentAdapter.submitList(recentList)

                        binding.tvEarlierHeader.visibility = if (earlierList.isNotEmpty()) View.VISIBLE else View.GONE
                        earlierAdapter.submitList(earlierList)
                    }
                }
                is UiState.Error -> {
                    showToast(state.message)
                }
                else -> {}
            }
        }

        viewModel.actionState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    showToast(state.data)
                }
                is UiState.Error -> {
                    showToast(state.message)
                }
                else -> {}
            }
        }
    }

    private fun showNotificationOptionsDialog(notification: NotificationDto) {
        val notificationId = notification.id ?: return
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = com.example.myapplication.databinding.DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.tvTitle.text = getString(R.string.dialog_notification_options_title)
        dialogBinding.tvMessage.text = "Bạn có chắc chắn muốn xóa thông báo này không?"
        dialogBinding.btnConfirm.text = "Xóa"
        dialogBinding.btnConfirm.setOnClickListener {
            viewModel.deleteNotification(notificationId)
            dialog.dismiss()
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun handleNotificationClick(notification: NotificationDto) {
        notification.id?.let { viewModel.markNotificationAsRead(it) }

        val type = notification.targetType?.uppercase() ?: notification.type?.uppercase()
        when (type) {
            "FRIEND", "FRIEND_REQUEST", "USER" -> {
                val targetId = notification.targetId ?: notification.actorId
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtra("target_user_id", targetId)
                }
                startActivity(intent)
            }
            "BOOKING", "APPOINTMENT" -> {
                val intent = Intent(this, ScheduleActivity::class.java).apply {
                    putExtra("booking_id", notification.targetId)
                }
                startActivity(intent)
            }
            "CHAT", "MESSAGE", "CONVERSATION" -> {
                val intent = Intent(this, FriendsListActivity::class.java)
                startActivity(intent)
            }
            else -> {
                if (!notification.actorId.isNullOrEmpty()) {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("target_user_id", notification.actorId)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}
