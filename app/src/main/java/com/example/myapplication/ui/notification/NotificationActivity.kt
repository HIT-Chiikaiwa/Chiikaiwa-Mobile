package com.example.myapplication.ui.notification

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.example.myapplication.data.remote.dto.response.NotificationDto
import com.example.myapplication.databinding.ActivityNotificationBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.schedule.ScheduleActivity
import com.example.myapplication.ui.profile.ProfileActivity

class NotificationActivity : BaseActivity<ActivityNotificationBinding>() {

    override fun inflateBinding() = ActivityNotificationBinding.inflate(layoutInflater)

    private val viewModel: NotificationViewModel by viewModels()

    private val recentAdapter by lazy {
        NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
    }

    private val earlierAdapter by lazy {
        NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
    }

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.rvRecentNotifications.adapter = recentAdapter
        binding.rvEarlierNotifications.adapter = earlierAdapter
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
                        binding.tvRecentHeader.visibility = View.GONE
                        binding.tvEarlierHeader.visibility = View.GONE
                        recentAdapter.submitList(emptyList())
                        earlierAdapter.submitList(emptyList())
                    } else {
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
    }

    private fun handleNotificationClick(notification: NotificationDto) {
        notification.id?.let { viewModel.markNotificationAsRead(it) }

        when (notification.targetType?.uppercase()) {
            "FRIEND", "FRIEND_REQUEST", "USER" -> {
                val targetId = notification.targetId ?: notification.actorId
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtra("target_user_id", targetId)
                }
                startActivity(intent)
            }
            "BOOKING", "APPOINTMENT" -> {
                val intent = Intent(this, ScheduleActivity::class.java)
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
