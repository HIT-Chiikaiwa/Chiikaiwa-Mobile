package com.example.myapplication.ui.profile

import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.ActivityRegistrationInfoBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiState
import java.util.Calendar
import java.util.Locale

class RegistrationInfoActivity : BaseActivity<ActivityRegistrationInfoBinding>() {

    override fun inflateBinding() = ActivityRegistrationInfoBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        setupGenderSpinner()

        val datePickerAction = {
            showDatePicker()
        }
        binding.etDateOfBirth.setOnClickListener { datePickerAction() }
        binding.btnPickDate.setOnClickListener { datePickerAction() }
    }

    private fun setupGenderSpinner() {
        val genders = listOf("Nam", "Nữ", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerGender.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val formattedDate = String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y)
            binding.etDateOfBirth.setText(formattedDate)
        }, year, month, day).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> bindUserData(state.data)
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }
    }

    private fun bindUserData(user: UserDto) {
        binding.etEmail.setText(user.email ?: "")
        binding.etId.setText(user.id)
        binding.etLastName.setText(user.lastName ?: "")
        binding.etFirstName.setText(user.firstName ?: "")

        when (user.gender?.uppercase(Locale.getDefault())) {
            "MALE" -> binding.spinnerGender.setSelection(0)
            "FEMALE" -> binding.spinnerGender.setSelection(1)
            else -> binding.spinnerGender.setSelection(2)
        }

        user.dateOfBirth?.let { dob ->
            binding.etDateOfBirth.setText(dob)
        }
    }
}
