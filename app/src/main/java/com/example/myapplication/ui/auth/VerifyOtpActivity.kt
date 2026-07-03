package com.example.myapplication.ui.auth

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import com.example.myapplication.databinding.VerifyOtpBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class VerifyOtpActivity : BaseActivity<VerifyOtpBinding>() {

    override fun inflateBinding() = VerifyOtpBinding.inflate(layoutInflater)

    private val viewModel: VerifyOtpViewModel by viewModels()

    private var email = ""

    override fun initView() {

        email = intent.getStringExtra("email") ?: ""
        binding.tvOtpDescription.text = "Mã OTP đã được gửi đến email:\n$email"
        binding.ivBack.setOnClickListener {
            finish()
        }

        setupOtpInputFocus()

        binding.btnVerify.setOnClickListener {
            binding.tvOtpError.visibility = View.GONE
            viewModel.verifyOtp(email, getOtpCode())
        }

        binding.btnResend.setOnClickListener {
            binding.tvOtpError.visibility = View.GONE
            viewModel.resendOtp(email)
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
                }

                is UiState.Error -> {
                    setLoading(false)

                    binding.tvOtpError.visibility = View.VISIBLE
                    binding.tvOtpError.text = state.message
                }
            }
        }

        viewModel.event.observe(this) { event ->

            when (event) {

                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }

                UiEvent.NavigateHome -> {

                    val intent = Intent(this, LoginActivity::class.java)

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {

        binding.btnVerify.isEnabled = !isLoading
        binding.btnResend.isEnabled = !isLoading

        if (isLoading) {
            binding.tvOtpError.visibility = View.GONE
        }
    }

    private fun getOtpCode(): String {

        return buildString {
            append(binding.edtOtp1.text.toString())
            append(binding.edtOtp2.text.toString())
            append(binding.edtOtp3.text.toString())
            append(binding.edtOtp4.text.toString())
            append(binding.edtOtp5.text.toString())
        }
    }

    private fun setupOtpInputFocus() {

        val editTexts = arrayOf(
            binding.edtOtp1,
            binding.edtOtp2,
            binding.edtOtp3,
            binding.edtOtp4,
            binding.edtOtp5
        )

        for (i in editTexts.indices) {

            val current = editTexts[i]

            current.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if (!s.isNullOrEmpty() && i < editTexts.lastIndex) {
                        editTexts[i + 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            current.setOnKeyListener { _, keyCode, event ->

                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (current.text.isEmpty() && i > 0) {

                        editTexts[i - 1].requestFocus()
                        editTexts[i - 1].setText("")

                        return@setOnKeyListener true
                    }
                }

                false
            }
        }
    }
}