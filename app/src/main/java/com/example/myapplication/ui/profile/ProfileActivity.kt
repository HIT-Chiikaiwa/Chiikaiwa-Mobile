package com.example.myapplication.ui.profile

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.FragmentProfileBinding
import com.example.myapplication.ui.auth.AuthActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileActivity : BaseActivity<FragmentProfileBinding>() {

    override fun inflateBinding() = FragmentProfileBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()
    private var targetUserId: String? = null
    private var currentProfile: UserDto? = null

    override fun initView() {
        targetUserId = intent.getStringExtra("target_user_id")
            ?: intent.getStringExtra("targetUserId")
            ?: intent.getStringExtra("userId")
            ?: intent.getStringExtra("id")
        val isOtherUser = !targetUserId.isNullOrEmpty() && targetUserId != viewModel.getUserId()

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.cvHonor.setOnClickListener {
            showSubjectManagementDialog(isOtherUser)
        }

        if (isOtherUser) {
            binding.btnUpdateProfile.visibility = View.GONE
            binding.cvSettings.visibility = View.GONE
            binding.ivWoodBottomLeft.visibility = View.GONE
            binding.ivWoodBottomRight.visibility = View.GONE
        } else {
            binding.btnUpdateProfile.visibility = View.VISIBLE
            binding.cvSettings.visibility = View.VISIBLE
            binding.ivWoodBottomLeft.visibility = View.VISIBLE
            binding.ivWoodBottomRight.visibility = View.VISIBLE

            binding.btnUpdateProfile.setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            binding.cvSettings.setOnClickListener {
                startActivity(Intent(this, AccountSettingsActivity::class.java))
            }
            val openScheduleAction = View.OnClickListener {
                val intent = Intent(this, com.example.myapplication.ui.home.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("type", "BOOKING")
                }
                startActivity(intent)
            }
            binding.cvAppointment.setOnClickListener(openScheduleAction)
            binding.layoutAppointmentInner.setOnClickListener(openScheduleAction)
            binding.tvAppointment.setOnClickListener(openScheduleAction)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile(targetUserId)
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Idle -> {
                }
                is UiState.Loading -> {
                    binding.btnUpdateProfile.isEnabled = false
                }
                is UiState.Success -> {
                    binding.btnUpdateProfile.isEnabled = true
                    currentProfile = state.data
                    bindProfile(state.data)
                }
                is UiState.Error -> {
                    binding.btnUpdateProfile.isEnabled = true
                    showToast(state.message)
                }
            }
        }

        viewModel.appointmentCount.observeState { count ->
            binding.tvAppointmentCount.text = "$count"
        }

        viewModel.subjects.observeState { subjectsList ->
            binding.tvSubjectCount.text = "${subjectsList.size}"
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }
                is UiEvent.NavigateHome -> {
                    val intent = Intent(this, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun bindProfile(user: UserDto) {
        val fullName = "${user.lastName ?: ""} ${user.firstName ?: ""}".trim()
        binding.tvUsername.text = fullName.ifEmpty { "Chưa cập nhật" }
        val trustScore = user.trustScore ?: 100.0
        binding.tvFavoriteRating.text = String.format(Locale.US, "%.1f", trustScore)
        binding.tvBuddyStatus.text = "Trạng thái quét: ${if (user.buddyActive == true) "Bật" else "Tắt"}"
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

        if (!user.avatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivAvatar)
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }
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

    private fun showSubjectManagementDialog(isOtherUser: Boolean = false) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).create()
        val binding = com.example.myapplication.databinding.DialogSubjectManagementBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        if (isOtherUser) {
            binding.tvTitle.text = "Danh Sách Môn Học"
            binding.btnAddSubject.visibility = View.GONE
        } else {
            binding.tvTitle.text = "Quản Lý Môn Học"
            binding.btnAddSubject.visibility = View.VISIBLE
            binding.btnAddSubject.setOnClickListener {
                showAddSubjectDialog {
                    viewModel.loadSubjects()
                }
            }
        }

        viewModel.subjects.observeState { subjectsList ->
            binding.layoutStrengthSubjects.removeAllViews()
            binding.layoutReviewSubjects.removeAllViews()

            for (sub in subjectsList) {
                val parent = if (sub.type == "STRENGTH") binding.layoutStrengthSubjects else binding.layoutReviewSubjects
                val itemBinding = com.example.myapplication.databinding.ItemDialogSubjectBinding.inflate(layoutInflater, parent, false)
                itemBinding.tvSubjectName.text = sub.name
                if (isOtherUser) {
                    itemBinding.btnDeleteSubject.visibility = View.GONE
                } else {
                    itemBinding.btnDeleteSubject.visibility = View.VISIBLE
                    itemBinding.btnDeleteSubject.setOnClickListener {
                        viewModel.deleteSubject(sub.id)
                    }
                }
                parent.addView(itemBinding.root)
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showAddSubjectDialog(onSubjectAdded: () -> Unit) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).create()
        val binding = com.example.myapplication.databinding.DialogAddSubjectBinding.inflate(layoutInflater)
        dialog.setView(binding.root)

        binding.btnCancelAdd.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirmAdd.setOnClickListener {
            val name = binding.etSubjectName.text.toString().trim()
            if (name.isEmpty()) {
                showToast("Vui lòng nhập tên môn học")
                return@setOnClickListener
            }

            val type = if (binding.rgSubjectType.checkedRadioButtonId == binding.rbStrength.id) "STRENGTH" else "NEED_REVIEW"
            viewModel.addSubject(name, type)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
