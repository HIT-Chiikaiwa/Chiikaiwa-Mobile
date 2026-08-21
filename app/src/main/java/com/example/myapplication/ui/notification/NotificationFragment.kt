package com.example.myapplication.ui.notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.databinding.FragmentNotificationBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.schedule.AppointmentReminderDialog
import com.example.myapplication.utils.extension.observeState

class NotificationFragment : BaseFragment<FragmentNotificationBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentNotificationBinding.inflate(inflater, container, false)

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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
        }

        binding.rvRecentNotifications.adapter = recentAdapter
        binding.rvEarlierNotifications.adapter = earlierAdapter

        checkAndShowReminderDialog()
    }

    private fun checkAndShowReminderDialog() {
        val showReminder = arguments?.getBoolean("show_reminder_dialog")
            ?: requireActivity().intent?.getBooleanExtra("show_reminder_dialog", false)
            ?: false

        if (showReminder) {
            val title = arguments?.getString("reminder_title")
                ?: requireActivity().intent?.getStringExtra("reminder_title")
                ?: "Cuộc hẹn"
            val scheduledAt = arguments?.getString("reminder_scheduled_at")
                ?: requireActivity().intent?.getStringExtra("reminder_scheduled_at")
                ?: ""
            AppointmentReminderDialog(requireContext(), title, scheduledAt).show()
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
        val dialog = AlertDialog.Builder(requireContext()).create()
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
                findNavController().navigate(
                    R.id.action_notificationFragment_to_profileFragment,
                    bundleOf("target_user_id" to targetId)
                )
            }
            "BOOKING", "APPOINTMENT" -> {
                findNavController().navigate(
                    R.id.action_notificationFragment_to_scheduleFragment,
                    bundleOf("booking_id" to notification.targetId)
                )
            }
            "CHAT", "MESSAGE", "CONVERSATION" -> {
                findNavController().navigate(R.id.action_notificationFragment_to_friendsListFragment)
            }
            else -> {
                if (!notification.actorId.isNullOrEmpty()) {
                    findNavController().navigate(
                        R.id.action_notificationFragment_to_profileFragment,
                        bundleOf("target_user_id" to notification.actorId)
                    )
                }
            }
        }
    }
}
