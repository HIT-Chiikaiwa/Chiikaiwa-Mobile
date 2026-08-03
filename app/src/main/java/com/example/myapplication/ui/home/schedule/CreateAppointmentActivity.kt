package com.example.myapplication.ui.home.schedule

import android.app.DatePickerDialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.myapplication.databinding.ActivityCreateAppointmentBinding
import com.example.myapplication.ui.base.BaseActivity
import java.util.Calendar
import java.util.Locale

class CreateAppointmentActivity : BaseActivity<ActivityCreateAppointmentBinding>() {

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    override fun inflateBinding() = ActivityCreateAppointmentBinding.inflate(layoutInflater)

    override fun initView() {
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
        setupModeSpinner()
        setupCreateButton()
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

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
        val calendar = Calendar.getInstance()

        binding.npHour.minValue = 0
        binding.npHour.maxValue = 23
        binding.npHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        binding.npHour.setFormatter { String.format(Locale.getDefault(), "%02d", it) }

        binding.npMinute.minValue = 0
        binding.npMinute.maxValue = 59
        binding.npMinute.value = calendar.get(Calendar.MINUTE)
        binding.npMinute.setFormatter { String.format(Locale.getDefault(), "%02d", it) }
    }

    private fun setupModeSpinner() {
        val modes = arrayOf("Offline", "Online")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
            if (binding.tvSelectedDate.text.isNullOrEmpty() || binding.tvSelectedDate.text == "Chọn ngày") {
                Toast.makeText(this, "Vui lòng chọn ngày hẹn", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.spMode.selectedItemPosition == 1) {
                val locationName = binding.etLocationName.text.toString().trim()
                if (locationName.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên địa điểm", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            Toast.makeText(this, "Tạo cuộc hẹn thành công!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun observeData() {
    }
}
