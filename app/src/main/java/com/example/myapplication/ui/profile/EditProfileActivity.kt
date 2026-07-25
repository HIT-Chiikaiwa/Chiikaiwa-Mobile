package com.example.myapplication.ui.profile

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.ActivityEditProfileBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    override fun inflateBinding() = ActivityEditProfileBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnUpdateProfile.setOnClickListener {
            showToast("Đã lưu thay đổi hồ sơ")
            finish()
        }

        binding.tvAddInfo.setOnClickListener {
            showSubjectManagementDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    bindProfile(state.data)
                }
                is UiState.Error -> {
                    showToast(state.message)
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

    private fun bindProfile(user: UserDto) {
        val fullName = "${user.lastName ?: ""} ${user.firstName ?: ""}".trim()
        binding.tvUsername.text = fullName.ifEmpty { "Chưa cập nhật" }
        binding.tvUserId.text = "ID: ${user.id}"
        binding.tvFriendsCount.text = "Điểm tin cậy: ${user.trustScore ?: 100.0} | Buddy: ${if (user.buddyActive == true) "Bật" else "Tắt"}"
        binding.tvIntroduction.text = user.statusTag ?: "Chưa có giới thiệu"

        val ageStr = calculateAge(user.dateOfBirth)
        binding.tvAge.text = "Tuổi: $ageStr"

        val genderStr = when (user.gender) {
            "MALE" -> "Nam"
            "FEMALE" -> "Nữ"
            else -> "Khác"
        }
        binding.tvGender.text = "Giới tính: $genderStr"

        binding.tvSchool.text = "Trường học: ${user.university ?: "Chưa cập nhật"}"
        binding.tvMajor.text = "Ngành học: ${user.majorName ?: "Chưa cập nhật"}"
        binding.tvCountry.text = "Quê quán: ${user.location ?: "Chưa cập nhật"}"

        Glide.with(this)
            .load(user.avatar)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(binding.imgAvatar)
    }

    private fun calculateAge(dateOfBirth: String?): String {
        if (dateOfBirth.isNullOrEmpty()) return "Chưa cập nhật"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val birthDate = sdf.parse(dateOfBirth) ?: return "Chưa cập nhật"
            val birthCalendar = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age.toString()
        } catch (e: Exception) {
            "Chưa cập nhật"
        }
    }

    private fun showSubjectManagementDialog() {
        showToast("Quản lý thông tin môn học bổ sung")
    }
}
