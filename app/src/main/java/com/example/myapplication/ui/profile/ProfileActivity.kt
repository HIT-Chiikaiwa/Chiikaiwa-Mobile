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
import com.example.myapplication.databinding.ActivityProfileBinding
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileActivity : BaseActivity<ActivityProfileBinding>() {

    override fun inflateBinding() = ActivityProfileBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()
    private var currentProfile: UserDto? = null

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnUpdateProfile.visibility = View.VISIBLE
        binding.btnUpdateProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.cvSettings.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        binding.cvFavorite.setOnClickListener {
            showSubjectManagementDialog()
        }

        val openScheduleAction = View.OnClickListener {
            startActivity(Intent(this, com.example.myapplication.ui.home.schedule.ScheduleActivity::class.java))
        }
        binding.cvAppointment.setOnClickListener(openScheduleAction)
        binding.layoutAppointmentInner.setOnClickListener(openScheduleAction)
        binding.tvAppointment.setOnClickListener(openScheduleAction)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Idle -> {
                    // Do nothing
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

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }
                is UiEvent.NavigateHome -> {
                    val intent = Intent(this, LoginActivity::class.java).apply {
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
        binding.tvFriendsCount.text = "Điểm tin cậy: ${user.trustScore ?: 100.0}"
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
        binding.tvCountry.text = "Quê quán: ${user.location ?: "Chưa cập nhật"}"

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

    private fun showSubjectManagementDialog() {
        val context = this
        val layout = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFCE2"))
        }
        layout.addView(container)

        val title = TextView(context).apply {
            text = "Quản Lý Môn Học"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        container.addView(title)

        val strengthLabel = TextView(context).apply {
            text = "Môn học thế mạnh (STRENGTH)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setPadding(0, 0, 0, 16)
        }
        container.addView(strengthLabel)

        val layoutStrengthSubjects = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(layoutStrengthSubjects, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })

        val reviewLabel = TextView(context).apply {
            text = "Môn học cần ôn tập (NEED_REVIEW)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setPadding(0, 0, 0, 16)
        }
        container.addView(reviewLabel)

        val layoutReviewSubjects = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(layoutReviewSubjects, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val btnCancel = Button(context).apply {
            text = "Đóng"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F4F0CA"))
        }
        buttonsLayout.addView(btnCancel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 16 })

        val btnAddSubject = Button(context).apply {
            text = "Thêm môn học"
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brown))
        }
        buttonsLayout.addView(btnAddSubject)

        container.addView(buttonsLayout)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(layout)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAddSubject.setOnClickListener {
            showAddSubjectDialog {
                viewModel.loadSubjects()
            }
        }

        viewModel.subjects.observeState { subjectsList ->
            layoutStrengthSubjects.removeAllViews()
            layoutReviewSubjects.removeAllViews()

            for (sub in subjectsList) {
                val itemView = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(36, 24, 36, 24)
                    setBackgroundResource(R.drawable.bg_edittext)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }
                }

                val tvSubjectName = TextView(context).apply {
                    text = sub.name
                    setTextColor(ContextCompat.getColor(context, R.color.brown))
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                itemView.addView(tvSubjectName)

                val ivDeleteSubject = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    contentDescription = "Xóa"
                    setPadding(18, 18, 18, 18)
                    setBackgroundResource(android.R.color.transparent)
                    imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error))
                    setOnClickListener {
                        viewModel.deleteSubject(sub.id)
                    }
                }
                itemView.addView(ivDeleteSubject)

                if (sub.type == "STRENGTH") {
                    layoutStrengthSubjects.addView(itemView)
                } else {
                    layoutReviewSubjects.addView(itemView)
                }
            }
        }

        dialog.show()
    }

    private fun showAddSubjectDialog(onSubjectAdded: () -> Unit) {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFCE2"))
        }

        val title = TextView(context).apply {
            text = "Thêm Môn Học"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)

        val edtSubjectName = EditText(context).apply {
            hint = "Tên môn học"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setHintTextColor(ContextCompat.getColor(context, R.color.hint))
            setBackgroundResource(R.drawable.bg_edittext)
            setPadding(24, 24, 24, 24)
        }
        layout.addView(edtSubjectName, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 })

        val typeLabel = TextView(context).apply {
            text = "Loại môn học:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(typeLabel)

        val rgSubjectType = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val rbStrength = android.widget.RadioButton(context).apply {
            id = View.generateViewId()
            text = "Thế mạnh"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            isChecked = true
        }
        rgSubjectType.addView(rbStrength, RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = 24 })

        val rbReview = android.widget.RadioButton(context).apply {
            id = View.generateViewId()
            text = "Cần ôn tập"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
        }
        rgSubjectType.addView(rbReview)

        layout.addView(rgSubjectType, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val btnCancelAdd = Button(context).apply {
            text = "Hủy"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F4F0CA"))
        }
        buttonsLayout.addView(btnCancelAdd, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 16 })

        val btnConfirmAdd = Button(context).apply {
            text = "Xác nhận"
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brown))
        }
        buttonsLayout.addView(btnConfirmAdd)

        layout.addView(buttonsLayout)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(layout)
            .create()

        btnCancelAdd.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmAdd.setOnClickListener {
            val name = edtSubjectName.text.toString().trim()
            if (name.isEmpty()) {
                showToast("Vui lòng nhập tên môn học")
                return@setOnClickListener
            }

            val type = if (rgSubjectType.checkedRadioButtonId == rbStrength.id) "STRENGTH" else "NEED_REVIEW"
            viewModel.addSubject(name, type)
            dialog.dismiss()
        }

        dialog.show()
    }
}
