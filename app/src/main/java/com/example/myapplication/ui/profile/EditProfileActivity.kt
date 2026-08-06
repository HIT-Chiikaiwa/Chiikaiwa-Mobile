package com.example.myapplication.ui.profile

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.request.UpdateAcademicInfoRequest
import com.example.myapplication.data.remote.dto.request.UpdatePersonalInfoRequest
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.ActivityEditProfileBinding
import com.example.myapplication.databinding.DialogEditIntroductionBinding
import com.example.myapplication.databinding.DialogEditPersonalInfoBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    override fun inflateBinding() = ActivityEditProfileBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()
    private var currentUserDto: UserDto? = null

    private val pickAvatarLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handleAvatarSelected(it) }
    }

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.imgEditAvatar.setOnClickListener {
            openImagePicker()
        }

        binding.imgAvatar.setOnClickListener {
            openImagePicker()
        }

        binding.btnUpdateProfile.setOnClickListener {
            showToast("Đã lưu thay đổi hồ sơ")
            finish()
        }

        val openEditInfoListener = {
            showEditPersonalInfoDialog()
        }
        binding.tvAddInfo.setOnClickListener { openEditInfoListener() }
        binding.ivTogglePassword1.setOnClickListener { openEditInfoListener() }

        val openEditIntroListener = {
            showEditIntroductionDialog()
        }
        binding.tvIntroduction.setOnClickListener { openEditIntroListener() }
        binding.ivTogglePassword.setOnClickListener { openEditIntroListener() }
    }

    private fun openImagePicker() {
        pickAvatarLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun handleAvatarSelected(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDimension = 800
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val (scaledWidth, scaledHeight) = if (width > maxDimension || height > maxDimension) {
                        if (width > height) {
                            maxDimension to (height * maxDimension / width)
                        } else {
                            (width * maxDimension / height) to maxDimension
                        }
                    } else {
                        width to height
                    }

                    val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                    val avatarFile = java.io.File(cacheDir, "avatar_compressed_${System.currentTimeMillis()}.jpg")
                    val fileOutputStream = java.io.FileOutputStream(avatarFile)
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()

                    withContext(Dispatchers.Main) {
                        viewModel.uploadAvatar(avatarFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Không thể đọc file ảnh")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Lỗi đọc file: ${e.localizedMessage}")
                }
            }
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
                    currentUserDto = state.data
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

        if (!user.avatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatar)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.imgAvatar)
        } else {
            binding.imgAvatar.setImageResource(R.drawable.ic_launcher_foreground)
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

    private fun showEditIntroductionDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = DialogEditIntroductionBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.etStatusTag.setText(currentUserDto?.statusTag ?: "")

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val newStatus = dialogBinding.etStatusTag.text.toString().trim()
            viewModel.updateStatusTag(newStatus)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showEditPersonalInfoDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = DialogEditPersonalInfoBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.etLastName.setText(currentUserDto?.lastName ?: "")
        dialogBinding.etFirstName.setText(currentUserDto?.firstName ?: "")
        dialogBinding.etDateOfBirth.setText(currentUserDto?.dateOfBirth ?: "")
        dialogBinding.etUniversity.setText(currentUserDto?.university ?: "")
        dialogBinding.etMajorName.setText(currentUserDto?.majorName ?: "")
        dialogBinding.etPhone.setText(currentUserDto?.phone ?: "")

        when (currentUserDto?.gender) {
            "MALE" -> dialogBinding.rbMale.isChecked = true
            "FEMALE" -> dialogBinding.rbFemale.isChecked = true
            else -> dialogBinding.rbOther.isChecked = true
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val lastName = dialogBinding.etLastName.text.toString().trim()
            val firstName = dialogBinding.etFirstName.text.toString().trim()
            val dob = dialogBinding.etDateOfBirth.text.toString().trim()
            val gender = when (dialogBinding.rgGender.checkedRadioButtonId) {
                dialogBinding.rbMale.id -> "MALE"
                dialogBinding.rbFemale.id -> "FEMALE"
                else -> "OTHER"
            }
            val university = dialogBinding.etUniversity.text.toString().trim()
            val majorName = dialogBinding.etMajorName.text.toString().trim()
            val phone = dialogBinding.etPhone.text.toString().trim()
            val email = currentUserDto?.email ?: ""

            viewModel.updateFullProfileInfo(
                personalRequest = UpdatePersonalInfoRequest(
                    firstName = firstName,
                    lastName = lastName,
                    gender = gender,
                    dateOfBirth = dob,
                    phone = phone,
                    email = email
                ),
                academicRequest = UpdateAcademicInfoRequest(
                    university = university,
                    majorName = majorName
                )
            )

            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
