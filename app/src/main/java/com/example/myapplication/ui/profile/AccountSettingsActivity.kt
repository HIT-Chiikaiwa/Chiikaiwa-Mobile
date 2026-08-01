package com.example.myapplication.ui.profile

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.request.ChangePasswordRequest
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.ActivityAccountSettingsBinding
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class AccountSettingsActivity : BaseActivity<ActivityAccountSettingsBinding>() {

    override fun inflateBinding() = ActivityAccountSettingsBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()
    private var currentUser: UserDto? = null

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.switchRadar.setOnCheckedChangeListener { _, isChecked ->
            val user = currentUser ?: return@setOnCheckedChangeListener
            if (isChecked != (user.buddyActive == true)) {
                viewModel.toggleBuddyStatus(isChecked)
            }
        }

        binding.cardPersonalInfo.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.cardNotificationSettings.setOnClickListener {
            showToast("Cài đặt thông báo: Tất cả thông báo đã được bật")
        }

        binding.cardHelp.setOnClickListener {
            showHelpDialog()
        }

        binding.cardLogout.setOnClickListener {
            viewModel.logout()
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
                    currentUser = state.data
                    binding.switchRadar.isChecked = state.data.buddyActive == true
                }
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> showToast(event.message)
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

    private fun showChangePasswordDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFCE2"))
        }

        val titleTextView = TextView(context).apply {
            text = "Đổi Mật Khẩu"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(titleTextView)

        val edtOldPassword = EditText(context).apply {
            hint = "Mật khẩu cũ"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setHintTextColor(ContextCompat.getColor(context, R.color.hint))
            setBackgroundResource(R.drawable.bg_edittext)
            setPadding(24, 24, 24, 24)
        }
        layout.addView(edtOldPassword, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 })

        val edtNewPassword = EditText(context).apply {
            hint = "Mật khẩu mới"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setHintTextColor(ContextCompat.getColor(context, R.color.hint))
            setBackgroundResource(R.drawable.bg_edittext)
            setPadding(24, 24, 24, 24)
        }
        layout.addView(edtNewPassword, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 })

        val edtConfirmNewPassword = EditText(context).apply {
            hint = "Xác nhận mật khẩu mới"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            setHintTextColor(ContextCompat.getColor(context, R.color.hint))
            setBackgroundResource(R.drawable.bg_edittext)
            setPadding(24, 24, 24, 24)
        }
        layout.addView(edtConfirmNewPassword, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val btnCancelPass = Button(context).apply {
            text = "Hủy"
            setTextColor(ContextCompat.getColor(context, R.color.brown))
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F4F0CA"))
        }
        buttonsLayout.addView(btnCancelPass, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 16 })

        val btnConfirmPass = Button(context).apply {
            text = "Xác nhận"
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brown))
        }
        buttonsLayout.addView(btnConfirmPass)

        layout.addView(buttonsLayout)

        val dialog = AlertDialog.Builder(context)
            .setView(layout)
            .create()

        btnCancelPass.setOnClickListener { dialog.dismiss() }
        btnConfirmPass.setOnClickListener {
            val oldPass = edtOldPassword.text.toString().trim()
            val newPass = edtNewPassword.text.toString().trim()
            val confirmPass = edtConfirmNewPassword.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                showToast("Vui lòng nhập đầy đủ thông tin")
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                showToast("Mật khẩu mới không trùng khớp")
                return@setOnClickListener
            }

            viewModel.changePassword(ChangePasswordRequest(oldPass, newPass, confirmPass))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Trợ giúp & Hỗ trợ")
            .setMessage("Mọi thắc mắc hoặc cần hỗ trợ kỹ thuật, vui lòng liên hệ nhóm phát triển qua email support@studydate.com.")
            .setPositiveButton("Đóng", null)
            .show()
    }
}
