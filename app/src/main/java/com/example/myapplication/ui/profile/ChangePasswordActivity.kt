package com.example.myapplication.ui.profile

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.example.myapplication.data.remote.dto.request.ChangePasswordRequest
import com.example.myapplication.databinding.ActivityChangePasswordBinding
import com.example.myapplication.ui.auth.ForgotPasswordActivity
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class ChangePasswordActivity : BaseActivity<ActivityChangePasswordBinding>() {

    override fun inflateBinding() = ActivityChangePasswordBinding.inflate(layoutInflater)

    private val viewModel: ProfileViewModel by viewModels()

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnOk.setOnClickListener {
            performChangePassword()
        }
    }

    private fun performChangePassword() {
        binding.tvOldPasswordError.visibility = View.GONE

        val oldPass = binding.etOldPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showToast("Vui lòng nhập đầy đủ thông tin")
            return
        }

        if (newPass != confirmPass) {
            showToast("Mật khẩu mới không trùng khớp")
            return
        }

        viewModel.changePassword(ChangePasswordRequest(oldPass, newPass, confirmPass))
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Error -> {
                    binding.tvOldPasswordError.visibility = View.VISIBLE
                    binding.tvOldPasswordError.text = "Mật khẩu không đúng. Vui lòng nhập lại."
                }
                else -> {}
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                    if (event.message.contains("thành công")) {
                        finish()
                    }
                }
                else -> {}
            }
        }
    }
}
