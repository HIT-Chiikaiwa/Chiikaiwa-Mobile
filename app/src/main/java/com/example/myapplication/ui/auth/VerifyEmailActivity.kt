package com.example.myapplication.ui.auth

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.example.myapplication.databinding.ActivityVerifyEmailBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class VerifyEmailActivity : BaseActivity<ActivityVerifyEmailBinding>() {

    override fun inflateBinding() = ActivityVerifyEmailBinding.inflate(layoutInflater)

    private val viewModel: VerifyEmailViewModel by viewModels()

    private var email = ""
    private var flow = "register"

    override fun initView() {
        email = intent.getStringExtra("email") ?: ""
        flow = intent.getStringExtra("flow") ?: "register"

        binding.tvDescription.text = "Nhấn Nhận mã OTP để nhận mã nhé!\nChíp sẽ gửi OTP tới $email"

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvLogin.setOnClickListener {
            viewModel.sendOtp(email, flow)
        }
    }

    override fun observeData() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                UiState.Idle -> {
                    setLoading(false)
                }
                UiState.Loading -> {
                    setLoading(true)
                }
                is UiState.Success -> {
                    setLoading(false)
                    startActivity(
                        Intent(this, VerifyOtpActivity::class.java).apply {
                            putExtra("email", email)
                            putExtra("flow", flow)
                        }
                    )
                }
                is UiState.Error -> {
                    setLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.event.observe(this) { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }
                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.tvLogin.isEnabled = !isLoading
        binding.tvLogin.alpha = if (isLoading) 0.5f else 1.0f
    }
}
