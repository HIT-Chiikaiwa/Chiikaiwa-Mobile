package com.example.myapplication.ui.profile

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.example.myapplication.R
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.request.UpdatePersonalInfoRequest
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.FragmentRegistrationInfoBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import java.util.Calendar
import java.util.Locale

class RegistrationInfoFragment : BaseFragment<FragmentRegistrationInfoBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRegistrationInfoBinding.inflate(inflater, container, false)

    private val viewModel: ProfileViewModel by viewModels()
    private var currentUserDto: UserDto? = null

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.etEmail.apply {
            keyListener = null
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
        }
        binding.etId.apply {
            keyListener = null
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
        }

        val savedEmail = PreferenceManager(requireContext()).getEmail()
        if (!savedEmail.isNullOrEmpty()) {
            binding.etEmail.setText(savedEmail)
        }

        setupGenderSpinner()

        val datePickerAction = {
            showDatePicker()
        }
        binding.etDateOfBirth.setOnClickListener { datePickerAction() }
        binding.btnPickDate.setOnClickListener { datePickerAction() }

        binding.btnSave.setOnClickListener {
            saveRegistrationInfo()
        }

        viewModel.loadProfile()
    }

    private fun setupGenderSpinner() {
        val genders = listOf("Nam", "Nữ", "Khác")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, genders).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerGender.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
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
                is UiState.Success -> {
                    currentUserDto = state.data
                    bindUserData(state.data)
                }
                is UiState.Error -> showToast(state.message)
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

    private fun bindUserData(user: UserDto) {
        val savedEmail = PreferenceManager(requireContext()).getEmail()
        val emailToDisplay = user.email.takeIf { !it.isNullOrEmpty() } ?: savedEmail ?: ""
        binding.etEmail.setText(emailToDisplay)
        binding.etId.setText(user.id)
        binding.etLastName.setText(user.lastName ?: "")
        binding.etFirstName.setText(user.firstName ?: "")

        when (user.gender?.uppercase(Locale.getDefault())) {
            "MALE", "NAM" -> binding.spinnerGender.setSelection(0)
            "FEMALE", "NỮ", "NU" -> binding.spinnerGender.setSelection(1)
            else -> binding.spinnerGender.setSelection(2)
        }

        binding.etDateOfBirth.setText(formatDateOfBirth(user.dateOfBirth))
    }

    private fun formatDateOfBirth(rawDob: String?): String {
        if (rawDob.isNullOrEmpty()) return ""
        return try {
            if (rawDob.contains("-")) {
                val datePart = rawDob.split("T")[0]
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    "${parts[2]}/${parts[1]}/${parts[0]}"
                } else {
                    rawDob
                }
            } else {
                rawDob
            }
        } catch (e: Exception) {
            rawDob
        }
    }

    private fun saveRegistrationInfo() {
        val lastName = binding.etLastName.text.toString().trim()
        val firstName = binding.etFirstName.text.toString().trim()
        val gender = when (binding.spinnerGender.selectedItemPosition) {
            0 -> "MALE"
            1 -> "FEMALE"
            else -> "OTHER"
        }
        val rawDob = binding.etDateOfBirth.text.toString().trim()
        val formattedDob = convertDobToIso(rawDob)

        val phone = currentUserDto?.phone ?: ""
        val savedEmail = PreferenceManager(requireContext()).getEmail()
        val email = currentUserDto?.email.takeIf { !it.isNullOrEmpty() } ?: savedEmail ?: ""

        val request = UpdatePersonalInfoRequest(
            firstName = firstName,
            lastName = lastName,
            gender = gender,
            dateOfBirth = formattedDob,
            phone = phone,
            email = email
        )

        viewModel.updatePersonalInfo(request)
    }

    private fun convertDobToIso(dobStr: String): String {
        if (dobStr.isEmpty()) return ""
        val parts = dobStr.split("/")
        return if (parts.size == 3) {
            "${parts[2]}-${parts[1]}-${parts[0]}"
        } else {
            dobStr
        }
    }
}
