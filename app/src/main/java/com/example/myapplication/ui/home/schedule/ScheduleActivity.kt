package com.example.myapplication.ui.home.schedule

import android.content.Intent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.BookingDto
import com.example.myapplication.databinding.FragmentScheduleBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleActivity : BaseActivity<FragmentScheduleBinding>() {

    private val viewModel: BookingViewModel by viewModels()
    private val adapter = ScheduleAdapter { booking ->
        BookingDetailDialog(this, booking, viewModel) { _, _, _ ->
            loadDataForCurrentWeek()
        }.show()
    }

    private val currentCalendar = Calendar.getInstance()
    private var selectedDayOfWeek = Calendar.MONDAY
    private var currentWeekStart: String = ""

    override fun inflateBinding() = FragmentScheduleBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.rvScheduleList.layoutManager = LinearLayoutManager(this)
        binding.rvScheduleList.adapter = adapter

        binding.btnCreateSchedule.setOnClickListener {
            startActivity(Intent(this, CreateAppointmentActivity::class.java))
        }

        setupWeekNavigation()
        setupDayTabs()

        loadDataForCurrentWeek()
    }

    private fun setupWeekNavigation() {
        binding.btnWeekPrev.setOnClickListener {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            loadDataForCurrentWeek()
        }

        binding.btnWeekNext.setOnClickListener {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            loadDataForCurrentWeek()
        }
    }

    private fun loadDataForCurrentWeek() {
        val weekCal = currentCalendar.clone() as Calendar
        weekCal.firstDayOfWeek = Calendar.MONDAY
        weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        currentWeekStart = sdf.format(weekCal.time)

        val monthYearFormat = SimpleDateFormat("'Tháng' M, yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthYearFormat.format(currentCalendar.time)

        viewModel.loadWeeklyBookings(currentWeekStart)
        viewModel.loadMyBookings()

        updateDayTabSelection(selectedDayOfWeek)
    }

    private fun setupDayTabs() {
        val dayMap = mapOf(
            binding.tabMon to Calendar.MONDAY,
            binding.tabTue to Calendar.TUESDAY,
            binding.tabWed to Calendar.WEDNESDAY,
            binding.tabThu to Calendar.THURSDAY,
            binding.tabFri to Calendar.FRIDAY,
            binding.tabSat to Calendar.SATURDAY,
            binding.tabSun to Calendar.SUNDAY
        )

        dayMap.forEach { (view, dayOfWeek) ->
            view.setOnClickListener {
                selectedDayOfWeek = dayOfWeek
                updateDayTabSelection(dayOfWeek)
                filterAndDisplayBookings()
            }
        }
    }

    private fun updateDayTabSelection(selectedDay: Int) {
        val tabs = listOf(
            binding.tabMon to Calendar.MONDAY,
            binding.tabTue to Calendar.TUESDAY,
            binding.tabWed to Calendar.WEDNESDAY,
            binding.tabThu to Calendar.THURSDAY,
            binding.tabFri to Calendar.FRIDAY,
            binding.tabSat to Calendar.SATURDAY,
            binding.tabSun to Calendar.SUNDAY
        )

        tabs.forEach { (textView, day) ->
            if (day == selectedDay) {
                textView.setBackgroundResource(R.drawable.bg_tab_selected)
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                textView.setBackgroundResource(R.drawable.bg_tab_unselected)
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }

        val weekCal = currentCalendar.clone() as Calendar
        weekCal.firstDayOfWeek = Calendar.MONDAY
        weekCal.set(Calendar.DAY_OF_WEEK, selectedDay)
        val tagFormat = SimpleDateFormat("dd/M", Locale.getDefault())
        binding.tvDateTag.text = tagFormat.format(weekCal.time)
    }

    private fun filterAndDisplayBookings() {
        val weeklyData = viewModel.weeklyBookings.value
        val allMyBookings = viewModel.myBookings.value

        val dayKeyList = when (selectedDayOfWeek) {
            Calendar.MONDAY -> listOf("MONDAY", "Monday", "0", "1")
            Calendar.TUESDAY -> listOf("TUESDAY", "Tuesday", "1", "2")
            Calendar.WEDNESDAY -> listOf("WEDNESDAY", "Wednesday", "2", "3")
            Calendar.THURSDAY -> listOf("THURSDAY", "Thursday", "3", "4")
            Calendar.FRIDAY -> listOf("FRIDAY", "Friday", "4", "5")
            Calendar.SATURDAY -> listOf("SATURDAY", "Saturday", "5", "6")
            Calendar.SUNDAY -> listOf("SUNDAY", "Sunday", "6", "7")
            else -> emptyList()
        }

        var filtered: List<BookingDto>? = null
        if (weeklyData?.daySchedules != null) {
            for (key in dayKeyList) {
                if (weeklyData.daySchedules.containsKey(key)) {
                    filtered = weeklyData.daySchedules[key]
                    break
                }
            }
        }

        if (filtered == null && allMyBookings.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetCal = currentCalendar.clone() as Calendar
            targetCal.firstDayOfWeek = Calendar.MONDAY
            targetCal.set(Calendar.DAY_OF_WEEK, selectedDayOfWeek)
            val targetDateStr = sdf.format(targetCal.time)

            filtered = allMyBookings.filter { booking ->
                booking.scheduledAt?.startsWith(targetDateStr) == true
            }
        }

        adapter.submitList(filtered ?: emptyList())
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.weeklyBookings.collectLatest {
                        filterAndDisplayBookings()
                    }
                }
                launch {
                    viewModel.myBookings.collectLatest {
                        filterAndDisplayBookings()
                    }
                }
            }
        }

        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }
    }
}
