package com.example.myapplication.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    override fun inflateBinding() = ActivityRegisterBinding.inflate(layoutInflater)

    private val viewModel: RegisterViewModel by viewModels()

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private var registeredEmail = ""

    override fun initView() {

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }

        val genders = arrayOf("Nam", "Nữ", "Khác")
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_gender,
            genders
        )

        adapter.setDropDownViewResource(R.layout.item_spinner_gender_dropdown)

        binding.spGender.adapter = adapter

        val dateClickListener = View.OnClickListener {
            showDatePicker()
        }

        binding.edtDate.setOnClickListener(dateClickListener)
        binding.ivCalendar.setOnClickListener(dateClickListener)

        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.ivToggleConfirmPassword.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        binding.tvRegister.setOnClickListener {

            binding.tvPasswordError.visibility = View.GONE

            val genderCode = when (binding.spGender.selectedItemPosition) {
                0 -> "MALE"
                1 -> "FEMALE"
                else -> "OTHER"
            }

            registeredEmail = binding.edtEmail.text.toString().trim()

            viewModel.register(
                lastName = binding.edtLastName.text.toString().trim(),
                firstName = binding.edtFirstName.text.toString().trim(),
                gender = genderCode,
                dateOfBirth = binding.edtDate.text.toString().trim(),
                email = registeredEmail,
                pass = binding.edtPassword.text.toString().trim(),
                confirmPass = binding.edtConfirmPassword.text.toString().trim()
            )
        }
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {

                UiState.Idle -> {
                    setLoading(false)
                }

                UiState.Loading -> {
                    setLoading(true)
                }

                is UiState.Success -> {
                    setLoading(false)
                    viewModel.resetState()
                    startActivity(
                        Intent(this, VerifyEmailActivity::class.java).apply {
                            putExtra("email", registeredEmail)
                        }
                    )
                }

                is UiState.Error -> {
                    setLoading(false)

                    binding.tvPasswordError.visibility = View.VISIBLE
                    binding.tvPasswordError.text = state.message
                }
            }
        }

        viewModel.event.observeEvent { event ->

            when (event) {

                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }

                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.tvRegister.isEnabled = !isLoading

        if (isLoading) {
            binding.tvPasswordError.visibility = View.GONE
        }
    }

    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val currentText = binding.edtDate.text.toString()

        if (currentText.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                sdf.parse(currentText)?.let {
                    calendar.time = it
                }

            } catch (_: Exception) {
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->

                val selectedCalendar = Calendar.getInstance()

                selectedCalendar.set(year, month, dayOfMonth)

                val sdf = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                binding.edtDate.setText(
                    sdf.format(selectedCalendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun togglePasswordVisibility() {

        if (isPasswordVisible) {
            binding.edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)

        } else {
            binding.edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
        }

        binding.edtPassword.setSelection(binding.edtPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun toggleConfirmPasswordVisibility() {

        if (isConfirmPasswordVisible) {

            binding.edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)

        } else {
            binding.edtConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)

        }
        binding.edtConfirmPassword.setSelection(binding.edtConfirmPassword.text.length)
        isConfirmPasswordVisible = !isConfirmPasswordVisible
    }
}