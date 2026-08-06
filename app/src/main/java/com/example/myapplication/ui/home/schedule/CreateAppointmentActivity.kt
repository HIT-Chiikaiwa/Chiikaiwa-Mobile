package com.example.myapplication.ui.home.schedule

import android.app.DatePickerDialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.example.myapplication.data.remote.dto.request.CreateBookingRequest
import com.example.myapplication.databinding.ActivityCreateAppointmentBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.R
import com.example.myapplication.utils.extension.setBrownTextColor
import java.util.Calendar
import java.util.Locale

class CreateAppointmentActivity : BaseActivity<ActivityCreateAppointmentBinding>() {

    private val viewModel: BookingViewModel by viewModels()

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var conversationId: String = ""

    override fun inflateBinding() = ActivityCreateAppointmentBinding.inflate(layoutInflater)

    private val durationValues = intArrayOf(30, 45, 60, 90, 120, 180, 240)

    override fun initView() {
        conversationId = intent.getStringExtra("conversation_id")
            ?: intent.getStringExtra("conversationId")
            ?: ""

        val userName = intent.getStringExtra("target_user_name")
            ?: intent.getStringExtra("user_name")
            ?: intent.getStringExtra("name")

        if (!userName.isNullOrEmpty()) {
            binding.tvScreenTitle.text = "Tạo cuộc hẹn với $userName"
        } else {
            binding.tvScreenTitle.text = "Tạo cuộc hẹn"
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        setupDatePicker()
        setupTimePickers()
        setupDurationSpinner()
        setupModeSpinner()
        setupCreateButton()
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
        }
        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

        binding.tvSelectedDate.text = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)

        binding.layoutDateField.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    binding.tvSelectedDate.text = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year)
                },
                selectedYear,
                selectedMonth,
                selectedDay
            )
            datePickerDialog.show()
        }
    }

    private fun setupTimePickers() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
        }

        binding.npHour.minValue = 0
        binding.npHour.maxValue = 23
        binding.npHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        binding.npHour.setFormatter { String.format(Locale.getDefault(), "%02d", it) }
        binding.npHour.setBrownTextColor()

        binding.npMinute.minValue = 0
        binding.npMinute.maxValue = 59
        binding.npMinute.value = calendar.get(Calendar.MINUTE)
        binding.npMinute.setFormatter { String.format(Locale.getDefault(), "%02d", it) }
        binding.npMinute.setBrownTextColor()
    }

    private fun setupDurationSpinner() {
        val durations = arrayOf(
            "30 phút",
            "45 phút",
            "60 phút (1 giờ)",
            "90 phút (1.5 giờ)",
            "120 phút (2 giờ)",
            "180 phút (3 giờ)",
            "240 phút (4 giờ)"
        )
        val adapter = ArrayAdapter(this, R.layout.item_spinner_selected, durations)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spDuration.adapter = adapter
        binding.spDuration.setSelection(0)
    }

    private fun setupModeSpinner() {
        val modes = arrayOf("Offline", "Online")
        val adapter = ArrayAdapter(this, R.layout.item_spinner_selected, modes)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spMode.adapter = adapter

        binding.spMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    binding.layoutLocationSection.visibility = View.VISIBLE
                } else {
                    binding.layoutLocationSection.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun setupCreateButton() {
        binding.btnCreate.setOnClickListener {
            binding.tvErrorMessage.visibility = View.GONE

            if (binding.tvSelectedDate.text.isNullOrEmpty() || binding.tvSelectedDate.text == "Chọn ngày") {
                binding.tvErrorMessage.text = "Vui lòng chọn ngày hẹn"
                binding.tvErrorMessage.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val isOffline = binding.spMode.selectedItemPosition == 0
            if (isOffline) {
                val locationName = binding.etLocationName.text.toString().trim()
                if (locationName.isEmpty()) {
                    binding.tvErrorMessage.text = "Vui lòng nhập tên địa điểm"
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    return@setOnClickListener
                }
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
                set(Calendar.HOUR_OF_DAY, binding.npHour.value)
                set(Calendar.MINUTE, binding.npMinute.value)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (cal.timeInMillis < System.currentTimeMillis() + (30 * 60 * 1000L - 60_000L)) {
                binding.tvErrorMessage.text = "Vui lòng chọn thời gian cách hiện tại ít nhất 30 phút"
                binding.tvErrorMessage.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val scheduledAt = com.example.myapplication.utils.TimeUtils.formatToUtc(cal.time)

            val selectedDuration = durationValues.getOrElse(binding.spDuration.selectedItemPosition) { 30 }

            val locationNameStr = if (isOffline) binding.etLocationName.text.toString().trim().ifEmpty { "Offline" } else "Online"
            val locationAddressStr = if (isOffline) binding.etStreet.text.toString().trim().ifEmpty { "Offline" } else "Online"
            val locationDistrictStr = if (isOffline) binding.etDistrict.text.toString().trim().ifEmpty { "Offline" } else "Online"
            val locationCityStr = if (isOffline) binding.etProvince.text.toString().trim().ifEmpty { "Offline" } else "Online"

            val request = CreateBookingRequest(
                subject = binding.tvScreenTitle.text.toString(),
                scheduledAt = scheduledAt,
                durationMinutes = selectedDuration,
                locationName = locationNameStr,
                locationAddress = locationAddressStr,
                locationDistrict = locationDistrictStr,
                locationCity = locationCityStr,
                note = "",
                isRecurring = binding.cbIsRecurring.isChecked,
                reminderMinutesBefore = 15
            )

            viewModel.createBooking(conversationId, request)
        }
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    finish()
                }
                is UiState.Error -> {
                    binding.tvErrorMessage.text = state.message
                    binding.tvErrorMessage.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> showToast(event.message)
                else -> {}
            }
        }
    }
}
