package com.example.myapplication.ui.home.schedule

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.request.CreateBookingRequest
import com.example.myapplication.databinding.FragmentCreateAppointmentBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.TimeUtils
import com.example.myapplication.utils.extension.observeState
import com.example.myapplication.utils.extension.setBrownTextColor
import java.util.Calendar
import java.util.Locale

class CreateAppointmentFragment : BaseFragment<FragmentCreateAppointmentBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCreateAppointmentBinding.inflate(inflater, container, false)

    private val viewModel: BookingViewModel by viewModels()

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var conversationId: String = ""

    private val durationValues = intArrayOf(30, 45, 60, 90, 120, 180, 240)

    override fun initView() {
        conversationId = arguments?.getString("conversation_id")
            ?: requireActivity().intent.getStringExtra("conversation_id")
            ?: requireActivity().intent.getStringExtra("conversationId")
            ?: ""

        val userName = arguments?.getString("target_user_name")
            ?: requireActivity().intent.getStringExtra("target_user_name")
            ?: requireActivity().intent.getStringExtra("user_name")
            ?: requireActivity().intent.getStringExtra("name")

        if (!userName.isNullOrEmpty()) {
            binding.tvScreenTitle.text = "Tạo cuộc hẹn với $userName"
        } else {
            binding.tvScreenTitle.text = "Tạo cuộc hẹn"
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupDatePicker()
        setupTimePickers()
        setupDurationSpinner()
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
                requireContext(),
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
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, durations)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spDuration.adapter = adapter
        binding.spDuration.setSelection(0)
    }

    private fun setupCreateButton() {
        binding.btnCreate.setOnClickListener {
            binding.tvErrorMessage.visibility = View.GONE

            if (binding.tvSelectedDate.text.isNullOrEmpty() || binding.tvSelectedDate.text == "Chọn ngày") {
                binding.tvErrorMessage.text = "Vui lòng chọn ngày hẹn"
                binding.tvErrorMessage.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val locationName = binding.etLocationName.text.toString().trim()
            if (locationName.isEmpty()) {
                binding.tvErrorMessage.text = "Vui lòng nhập tên địa điểm"
                binding.tvErrorMessage.visibility = View.VISIBLE
                return@setOnClickListener
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

            val scheduledAt = TimeUtils.formatToUtc(cal.time)

            val selectedDuration = durationValues.getOrElse(binding.spDuration.selectedItemPosition) { 30 }

            val locationNameStr = binding.etLocationName.text.toString().trim().ifEmpty { "Offline" }
            val locationAddressStr = binding.etStreet.text.toString().trim().ifEmpty { "Offline" }
            val locationDistrictStr = binding.etDistrict.text.toString().trim().ifEmpty { "Offline" }
            val locationCityStr = binding.etProvince.text.toString().trim().ifEmpty { "Offline" }

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
                    findNavController().navigateUp()
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
